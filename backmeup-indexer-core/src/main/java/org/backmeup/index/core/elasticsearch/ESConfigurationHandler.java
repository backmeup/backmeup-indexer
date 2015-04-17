package org.backmeup.index.core.elasticsearch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SystemUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.backmeup.index.UserDataWorkingDir;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.core.elasticsearch.tokenreader.MapTokenResolver;
import org.backmeup.index.core.elasticsearch.tokenreader.TokenReplaceReader;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.model.User;
import org.backmeup.index.utils.cmd.CommandLineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESConfigurationHandler {

    private static final Logger log = LoggerFactory.getLogger(ESConfigurationHandler.class);

    private static RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10 * 1000)
            .setSocketTimeout(10 * 1000).build();

    private static int TCPPORT_MIN = 9300;
    private static int TCPPORT_MAX = 9399;

    private static int HTTPPORT_MIN = 9200;
    private static int HTTPPORT_MAX = 9299;

    /**
     * The elasticsearch_template.yml file contains tokens for http and tcp port configuration which need to be replaced
     * with the individual user specific configuration -> which gets written to disc. The data and log file directory
     * (written in the config file) is either a standard working dir or a mounted truecrypt container (if one is
     * available)
     * 
     * @param tcpport
     *            accepted port range from 9300 to 9399
     * @param httpport
     *            accepted port range from 9200 to 9299
     * @returns file pointer to the created configuration file
     */
    public static File createUserYMLStartupFile(User userID, URL host, int tcpport, int httpport, String mountedTCVolume)
            throws IOException, ExceptionInInitializerError, NumberFormatException {

        checkPortRangeAccepted(tcpport, httpport);

        // the tokens within the elasticsearch_template.yml file to replace
        Map<String, String> tokens = new HashMap<>();
        tokens.put("tcpport", tcpport + "");
        tokens.put("httpport", httpport + "");
        tokens.put("clustername", "user" + userID);
        // info marvel does not allow to specify the protocol as http
        tokens.put("marvelagent", host.getHost() + ":" + httpport + "");
        log.debug("creating yml configuration file with ports tcp: " + tcpport + " http: " + httpport
                + " clustername: user" + userID);
        // check if a Truecrypt Volume has been mounted, if not use the default
        // working dir path
        if (mountedTCVolume != null) {
            String pathtologs = "";
            String pathtodata = "";
            if (SystemUtils.IS_OS_LINUX) {
                pathtologs = mountedTCVolume + "/index/index-logs";
                pathtodata = mountedTCVolume + "/index/index-data";
            }
            if (SystemUtils.IS_OS_WINDOWS) {
                pathtologs = mountedTCVolume + ":" + "/index/index-logs";
                pathtodata = mountedTCVolume + ":" + "/index/index-data";
            }
            File fileLogs = new File(pathtologs);
            fileLogs.mkdirs();
            File fileData = new File(pathtodata);
            fileData.mkdirs();
            log.debug("creating data + log on mounted TC volume" + pathtodata + " and " + pathtologs);
            tokens.put("pathtologs", pathtologs);
            tokens.put("pathtodata", pathtodata);
        } else {
            log.debug("creating data + log on standard volume" + UserDataWorkingDir.getDir(userID));
            tokens.put("pathtologs", UserDataWorkingDir.getDir(userID) + "/index/index-logs");
            tokens.put("pathtodata", UserDataWorkingDir.getDir(userID) + "/index/index-data");
        }

        MapTokenResolver resolver = new MapTokenResolver(tokens);

        ClassLoader classLoader = ESConfigurationHandler.class.getClassLoader();
        InputStream classpathResource = classLoader.getResourceAsStream("elasticsearch_template.yml");
        try (Reader inputReader = new InputStreamReader(classpathResource)) {

            try (Reader tokenReplaceReader = new TokenReplaceReader(inputReader, resolver)) {
                String outputFile = UserDataWorkingDir.getDir(userID) + "/index/elasticsearch.config.user" + userID
                        + ".yml";

                File file = new File(outputFile);
                file.getParentFile().mkdirs();

                try (Writer outputStream = new FileWriter(file)) {
                    int c;
                    while ((c = tokenReplaceReader.read()) != -1) {
                        outputStream.write(c);
                    }
                    return file;
                }
            }
        }
    }

    /**
     * Starts an elastic search instance for an existing configuration
     */
    public static int startElasticSearch(User userID) throws IOException, InterruptedException {

        // TODO add -server to the command line to not use the client vm (better performance)
        String command = null;
        if (SystemUtils.IS_OS_LINUX) {
            command = "sudo " + getElasticSearchExecutable() + " " + "-Des.config=" + UserDataWorkingDir.getDir(userID)
                    + "/index/elasticsearch.config.user" + userID + ".yml";
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            command = "\"" + getElasticSearchExecutable() + "\"" + " " + "-Des.config="
                    + UserDataWorkingDir.getDir(userID) + "/index/elasticsearch.config.user" + userID + ".yml";
        }
        log.debug(command);

        try {
            // TODO use ProcessBuilder instead and assign a dedicated amount of memory
            Process p = Runtime.getRuntime().exec(command);
            //give ES a chance to startup before returning - wait 10 seconds
            Thread.sleep(10000);
            // p.waitFor();
            return CommandLineUtils.getExecPID(p);
        } catch (IOException e) {
            log.error("Error executing: " + command + " " + e.toString());
            throw e;
        }
    }

    public static void stopElasticSearch(User user, RunningIndexUserConfig config) throws IOException {
        if (config != null) {
            HttpPost shutdownRequest = new HttpPost(config.getHostAddress() + ":" + config.getHttpPort() + "/_shutdown");
            shutdownElasticSearch(shutdownRequest);
            try {
                //Give ES a chance to properly terminate
                Thread.sleep(3000);
            } catch (InterruptedException e1) {
            }

            //check if Process has terminated, if not kill it
            if (config.getEsPID() != -1) {
                boolean isPIDrunning = CommandLineUtils.isProcessRunning(config.getEsPID(), 2, TimeUnit.SECONDS);
                log.debug("checking ES process for userID: " + user.id() + " still running?: " + isPIDrunning);
                if (isPIDrunning) {
                    try {
                        CommandLineUtils.killProcess(config.getEsPID(), 2, TimeUnit.SECONDS);
                        //recheck if process was properly terminated
                        if (CommandLineUtils.isProcessRunning(config.getEsPID(), 2, TimeUnit.SECONDS)) {
                            log.warn("unable to terminate ES PID: " + config.getEsPID() + " for userID: " + user.id());
                        }
                    } catch (Exception e) {
                        String s = "stopElasticSearch for userID " + user.id() + " could not killProcess PID: "
                                + config.getEsPID() + " due to" + e.toString();
                        log.debug(s);
                        throw new IOException(s, e);
                    }
                }
            } else {
                log.debug("stopElasticSearch for userID " + user.id()
                        + " missing Process PID for ES instance, no forced termination possible");
            }
        } else {
            String s = "stopElasticSearch for userID " + user.id() + " failed due to missing RunningIndexUserConfig";
            log.debug(s);
            throw new IOException(s);
        }
    }

    private static void shutdownElasticSearch(HttpPost shutdownRequest) throws IOException {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build()) {
            log.debug("Issuing shutdown request: " + shutdownRequest + " to ES");
            try (CloseableHttpResponse response = httpClient.execute(shutdownRequest)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    log.debug("shutdown ES properly initialized - status code 200");
                    httpClient.close();
                } else {
                    httpClient.close();
                    log.debug("shutdown ES failed with statuscode: " + response.getStatusLine().getStatusCode());
                    throw new IOException("ES shutdown command failed, statuscode:"
                            + response.getStatusLine().getStatusCode());
                }
            }
        }
    }

    /**
     * Issues the shutdown command to all running elastic search instances
     */
    public static void stopAllRude() {
        // iterate over the range of all possible ports
        for (int i = HTTPPORT_MIN; i <= HTTPPORT_MAX; i++) {
            HttpPost shutdownRequest = new HttpPost("http://localhost:" + i + "/_shutdown");
            try {
                shutdownElasticSearch(shutdownRequest);
            } catch (IOException e) {
            }
        }
    }

    public static boolean isElasticSearchInstanceRunning(URL host, int httpPort) throws IOException {

        if ((host != null) && (host.getProtocol() != null) && (host.getHost() != null) && (httpPort > -1)) {
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig)
                    .build()) {
                HttpGet healthyRequest = new HttpGet(host.getProtocol() + "://" + host.getHost() + ":" + httpPort
                        + "/_cluster/health?pretty=true");

                log.debug("calling: " + healthyRequest);
                try (CloseableHttpResponse response = httpClient.execute(healthyRequest)) {
                    log.debug("response: " + response.toString());
                    return response.getStatusLine().getStatusCode() == 200;
                }
            }
        }
        throw new IOException("specified host: " + host + " and port: " + httpPort + " may not be null");
    }

    public static String getElasticSearchExecutable() throws ExceptionInInitializerError {
        String s = Configuration.getProperty("elasticsearch.home.dir");
        if (s != null && s.length() > 0 && !s.contains("\"")) {
            File f = new File(s);
            if (f.isDirectory() && f.exists()) {
                String tcexe = null;

                if (SystemUtils.IS_OS_LINUX) {
                    tcexe = f.getAbsolutePath() + "/bin/elasticsearch";
                }
                if (SystemUtils.IS_OS_WINDOWS) {
                    tcexe = f.getAbsolutePath() + "/bin/elasticsearch.bat";
                }
                File tc = new File(tcexe);
                if (tc.isFile() && tc.exists()) {
                    return tc.getAbsolutePath();
                }
            }
        }
        throw new ExceptionInInitializerError("Error finding ElasticSearch executable in " + s);
    }

    public static boolean checkElasticSearchAvailable() {
        try {
            getElasticSearchExecutable();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void checkPortRangeAccepted(int tcpport, int httpport) throws NumberFormatException {
        if (tcpport < TCPPORT_MIN || tcpport > TCPPORT_MAX) {
            throw new NumberFormatException("Provided ElasticSearch tcpport " + tcpport + " is out of accepted range "
                    + TCPPORT_MIN + "-" + TCPPORT_MAX);
        }
        if (httpport < HTTPPORT_MIN || httpport > HTTPPORT_MAX) {
            throw new NumberFormatException("Provided ElasticSearch httpport " + httpport
                    + " is out of accepted range " + HTTPPORT_MIN + "-" + HTTPPORT_MAX);
        }
    }

}
