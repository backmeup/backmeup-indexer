package org.backmeup.index.core.elasticsearch;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search provider handles the index related operation but delegates to a low level handler for the real work.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@ApplicationScoped
public class SearchInstances {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // TODO @see ESConfigurationHandler.checkPortRangeAccepted - these values
    // are currently hard coded there

    private final Map<URL, AvailableESInstanceState> availableESInstances = new HashMap<>();
    private URL defaultHost;

    public URL getDefaultHost() {
        return this.defaultHost;
    }

    /**
     * For now this information is static and only synced with running records stored within the DB. TODO - add the
     * option for cluster configuration in property file - central connection to all cluster instances - lightweight
     * module to start/stop instances on each cluster instance
     */
    @PostConstruct
    public void initAvailableInstances() {
        try {
            this.defaultHost = new URI("http", InetAddress.getLocalHost().getHostAddress(), null, null).toURL();
        } catch (MalformedURLException | UnknownHostException | URISyntaxException e) {
            throw new IllegalArgumentException("InetAddress.getLocalHost().getHostAddress()", e);
        }

        this.availableESInstances.clear();
        this.availableESInstances.put(this.defaultHost, portsForDefaultIndexNode());
    }

    private AvailableESInstanceState portsForDefaultIndexNode() {
        List<Integer> supportedTcpPorts = new ArrayList<>();
        for (int i = 9360; i <= 9399; i++) {
            supportedTcpPorts.add(i);
        }

        // init the available port range on elasticsearch
        // Note: @see ESConfigurationHandler.checkPortRangeAccepted - these
        // values are currently also hardcoded there
        // TODO reset the port range to 9300 and 9200
        List<Integer> supportedHttpPorts = new ArrayList<>();
        for (int i = 9260; i <= 9299; i++) {
            supportedHttpPorts.add(i);
        }
        AvailableESInstanceState esInstance1 = new AvailableESInstanceState(supportedTcpPorts, supportedHttpPorts);
        return esInstance1;
    }

    public RunningIndexUserConfig createIndexUserConfig(User userID, File fTCContainer, String tcMountedDriveLetter) {
        try {
            // TODO currently when all available ports are in use the system will throw a NumberFormatException
            int tcpPort = getFreeESTCPPort();
            int httpPort = getFreeESHttpPort();

            // TODO currently only one host machine for ES supported: localhost
            URI uri = new URI("http", InetAddress.getLocalHost().getHostAddress() + "", "", "");
            // keep a database record of this configuration
            return new RunningIndexUserConfig(userID, uri.toURL(), tcpPort, httpPort, "user" + userID,
                    tcMountedDriveLetter, fTCContainer.getAbsolutePath());

        } catch (URISyntaxException | UnknownHostException | MalformedURLException e1) {
            throw new SearchInstanceException("startupInstance for userID: " + userID + " step5 - failed", e1);
        }
    }

    private int getFreeESHttpPort() {
        // TODO Loadbalancing between the different host machines
        return this.availableESInstances.get(this.defaultHost).useNextHTTPPort();
    }

    private int getFreeESTCPPort() {
        // TODO Loadbalancing between the different host machines
        return this.availableESInstances.get(this.defaultHost).useNextTCPPort();
    }

    public boolean isKnownHost(URL hostAddress) {
        return this.availableESInstances.get(hostAddress) != null;
    }

    public void takeHostPorts(RunningIndexUserConfig config) {
        this.availableESInstances.get(config.getHostAddress()).removeAvailableHTTPPort(config.getHttpPort());
        this.availableESInstances.get(config.getHostAddress()).removeAvailableTCPPort(config.getTcpPort());
    }

    public void createIndexStartFile(RunningIndexUserConfig runningConfig) {
        User userID = runningConfig.getUser();
        try {
            ESConfigurationHandler.createUserYMLStartupFile(userID, this.defaultHost, runningConfig.getTcpPort(),
                    runningConfig.getHttpPort(), runningConfig.getMountedTCDriveLetter());
            this.log.debug("startupInstance for userID: " + userID + " step4 - ok");
        } catch (NumberFormatException | ExceptionInInitializerError | IOException e1) {
            throw new SearchInstanceException("startupInstance for userID: " + userID + " step4 - failed", e1);
        }
    }

    public int startIndexNode(RunningIndexUserConfig runningConfig) {
        User userID = runningConfig.getUser();
        try {
            int pid = ESConfigurationHandler.startElasticSearch(userID);
            this.log.debug("startupInstance for userID: " + userID + " step6 - ok");
            this.log.info("started ES Instance " + runningConfig.getClusterName() + " on host: "
                    + runningConfig.getHostAddress().getHost() + ":" + runningConfig.getHttpPort());
            return pid;
        } catch (IOException | InterruptedException e1) {
            throw new SearchInstanceException("startupInstance for userID: " + userID + " step6 - failed", e1);
        }
    }

    public void shutdownIndexNode(RunningIndexUserConfig config) {
        User userID = config.getUser();
        try {
            ESConfigurationHandler.stopElasticSearch(userID, config);
            this.log.debug("shutdownInstance for userID: " + userID + " step2 - ok");
        } catch (IOException e) {
            this.log.debug("shutdownInstance for userID: " + userID + " step2 - failed", e);
        }
    }

    /**
     * Cleans up the available and used port mapping. This method does not stop running ES and TC instances
     */
    public void releaseHostPorts(RunningIndexUserConfig config) {
        this.availableESInstances.get(this.defaultHost).addAvailableHTTPPort(config.getHttpPort());
        this.availableESInstances.get(this.defaultHost).addAvailableTCPPort(config.getTcpPort());
    }

    public void shutdownAllIndexNodes() {
        this.log.debug("cleanupRude: started stopping all ES instances");
        // shutdown all elastic search instances
        ESConfigurationHandler.stopAllRude();
        this.log.debug("cleanupRude: completed - no ES instances running");
    }

}
