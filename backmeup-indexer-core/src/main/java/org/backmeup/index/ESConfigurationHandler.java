package org.backmeup.index;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.db.RunningIndexUserConfig;
import org.backmeup.index.utils.tokenreader.MapTokenResolver;
import org.backmeup.index.utils.tokenreader.TokenReplaceReader;

public class ESConfigurationHandler {

	public static int TCPPORT_MIN = 9300;
	public static int TCPPORT_MAX = 9399;

	public static int HTTPPORT_MIN = 9200;
	public static int HTTPPORT_MAX = 9299;

	/**
	 * The elasticsearch_template.yml file contains tokens for http and tcp port
	 * configuration which need to be replaced with the individual user specific
	 * configuration -> which gets written to disc. The data and log file
	 * directory (written in the config file) is either a standard working dir
	 * or a mounted truecrypt container (if one is available)
	 * 
	 * @param tcpport
	 *            accepted port range from 9300 to 9399
	 * @param httpport
	 *            accepted port range from 9200 to 9299
	 * @returns file pointer to the created configuration file
	 */
	public static File createUserYMLStartupFile(int userID, URL host,
			int tcpport, int httpport, String mountedTCVolume)
			throws IOException, ExceptionInInitializerError,
			NumberFormatException {

		checkPortRangeAccepted(tcpport, httpport);

		// the tokens within the elasticsearch_template.yml file to replace
		Map<String, String> tokens = new HashMap<>();
		tokens.put("tcpport", tcpport + "");
		tokens.put("httpport", httpport + "");
		tokens.put("clustername", "user" + userID);
		tokens.put("marvelagent", host + ":" + httpport + "");
		// check if a Truecrypt Volume has been mounted, if not use the default
		// working dir path
		if (mountedTCVolume != null) {
			// TODO we're having problems with the IndexManager here -
			// .getTCMountedVolume(userID) returns null here when it shouldn't
			System.out.println("creating data + log on mounted TC Volume");
			tokens.put("pathtologs", mountedTCVolume + "/index/index-logs");
			tokens.put("pathtodata", mountedTCVolume + "/index/index-data");
		} else {
			System.out.println("creating data + log on standard Volume");
			tokens.put("pathtologs", IndexManager.getUserDataWorkingDir(userID)
					+ "/index/index-logs");
			tokens.put("pathtodata", IndexManager.getUserDataWorkingDir(userID)
					+ "/index/index-data");
		}

		MapTokenResolver resolver = new MapTokenResolver(tokens);

		Reader inputReader = new FileReader(new File(
				"src/main/resources/elasticsearch_template.yml"));
		Reader tokenReplaceReader = new TokenReplaceReader(inputReader,
				resolver);
		String outputFile = IndexManager.getUserDataWorkingDir(userID)
				+ "/index/elasticsearch.config.user" + userID + ".yml";
		File file = new File(outputFile);
		file.getParentFile().mkdirs();

		Writer outputStream = new FileWriter(file);
		try {
			int c;
			while ((c = tokenReplaceReader.read()) != -1) {
				outputStream.write(c);
			}
		} finally {
			if (tokenReplaceReader != null) {
				tokenReplaceReader.close();
			}
			if (inputReader != null) {
				inputReader.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}
		return file;

	}

	/**
	 * Starts an elastic search instance for an existing configuration
	 */
	public static void startElasticSearch(int userID) throws IOException,
			InterruptedException {

		// TODO add -server to the command line to not use the client vm (better
		// performance)
		String command = "\"" + getElasticSearchExecutable() + "\"" + " "
				+ "-Des.config=" + IndexManager.getUserDataWorkingDir(userID)
				+ "/index/elasticsearch.config.user" + userID + ".yml";
		System.out.println(command);

		try {
			// TODO use ProcessBuilder instead and assign a dedicated amount of
			// memory
			Process p = Runtime.getRuntime().exec(command);
			Thread.currentThread();
			// give ES a chance to startup before returning - wait 10 seconds
			Thread.sleep(10000);
			// p.waitFor();
			// TODO need to kill this process when shutdown / cleanup is called?

		} catch (IOException e) {
			System.out.println("Error executing: " + command + " "
					+ e.toString());
			throw e;
		}
	}

	public static void stopElasticSearch(int userID)
			throws ClientProtocolException, IOException {
		// TODO need to implement. get Process and call quit.
		RunningIndexUserConfig config = IndexManager.getInstance()
				.getRunningIndexUserConfig(userID);
		HttpGet shutdownRequest = new HttpGet(config.getHostAddress() + ":"
				+ config.getHttpPort() + "/_shutdown");
		shutdownElasticSearch(shutdownRequest);
	}

	private static void shutdownElasticSearch(HttpGet shutdownRequest)
			throws IOException {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		System.out.println("Issuing shutdown request: " + shutdownRequest);
		HttpResponse response = httpClient.execute(shutdownRequest);
		if (response.getStatusLine().getStatusCode() != 201) {
			throw new IOException("ES shutdown command failed, statuscode:"
					+ response.getStatusLine().getStatusCode());
		}
	}

	/**
	 * Issues the shutdown command to all running elastic search instances
	 */
	public static void stopAll() {
		// iterate over the range of all possible ports
		for (int i = HTTPPORT_MIN; i <= HTTPPORT_MAX; i++) {
			HttpGet shutdownRequest = new HttpGet("http://localhost:" + i
					+ "/_shutdown");
			try {
				shutdownElasticSearch(shutdownRequest);
			} catch (IOException e) {
			}
		}
	}

	public static boolean isElasticSearchInstanceRunning(URL host, int httpPort)
			throws IOException {

		if ((host.getProtocol() != null) && (host.getHost() != null)
				&& (httpPort > -1)) {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet healthyRequest = new HttpGet(host.getProtocol() + "://"
					+ host.getHost() + ":" + httpPort
					+ "/_cluster/health?pretty=true");

			HttpResponse response;
			System.out.println("calling: " + healthyRequest);

			response = httpClient.execute(healthyRequest);
			System.out.println(response.toString());

			if (response.getStatusLine().getStatusCode() == 200) {
				return true;
			}
			return false;
		}
		throw new IOException("specified host: " + host + " and port: "
				+ httpPort + " may not be null");
	}

	public static String getElasticSearchExecutable()
			throws ExceptionInInitializerError {
		String s = Configuration.getProperty("elasticsearch.home.dir");
		if (s != null && s.length() > 0 && !s.contains("\"")) {
			File f = new File(s);
			if (f.isDirectory() && f.exists()) {
				String tcexe = f.getAbsolutePath() + "/bin/elasticsearch.bat";
				File tc = new File(tcexe);
				if (tc.isFile() && tc.exists()) {
					return tc.getAbsolutePath();
				}
			}
		}
		throw new ExceptionInInitializerError("Error finding ElasticSearch in "
				+ s);
	}

	public static boolean checkElasticSearchAvailable() {
		String s = Configuration.getProperty("elasticsearch.home.dir");
		if (s != null && s.length() > 0 && !s.contains("\"")) {
			File f = new File(s);
			if (f.isDirectory() && f.exists()) {
				String tcexe = f.getAbsolutePath() + "/bin/elasticsearch.bat";
				File tc = new File(tcexe);
				if (tc.isFile() && tc.exists()) {
					return true;
				}
			}
		}
		return false;
	}

	private static void checkPortRangeAccepted(int tcpport, int httpport)
			throws NumberFormatException {
		if (tcpport < TCPPORT_MIN || tcpport > TCPPORT_MAX) {
			throw new NumberFormatException("Provided ElasticSearch tcpport "
					+ tcpport + " is out of accepted range " + TCPPORT_MIN
					+ "-" + TCPPORT_MAX);
		}
		if (httpport < HTTPPORT_MIN || httpport > HTTPPORT_MAX) {
			throw new NumberFormatException("Provided ElasticSearch httpport "
					+ httpport + " is out of accepted range " + HTTPPORT_MIN
					+ "-" + HTTPPORT_MAX);
		}
	}

	public static String getESHomePath() throws ExceptionInInitializerError {
		String s = Configuration.getProperty("elasticsearch.home.dir");

		if (s != null && s.length() > 0 && !s.contains("\"")) {
			File f = new File(s);
			if (f.isDirectory() && f.exists()) {
				return f.getAbsolutePath();
			} else {
				throw new ExceptionInInitializerError(
						"ElasticSearch home.dir does not exist or is not accessible to system");
			}
		}
		throw new ExceptionInInitializerError(
				"ElasticSearch Home not properly configured within backmeup-indexer.properties");
	}

	/**
	 * Returns a pseudo-random number between min and max, inclusive.
	 * 
	 * @param min
	 *            Minimum value
	 * @param max
	 *            Maximum value. Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	private static int randInt(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

}
