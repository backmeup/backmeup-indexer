package org.backmeup.index;

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
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.index.config.AvailableESInstanceState;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.dal.DataAccessLayer;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.dal.jpa.DataAccessLayerImpl;
import org.backmeup.index.db.RunningIndexUserConfig;
import org.backmeup.index.utils.file.FileUtils;
import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexManager {

    public static IndexManager getInstance() {
        if (im == null) {
            //add synchronization over threads due to IndexCoreGargabgeCollector
            synchronized (IndexManager.class) {
                if (im == null)
                    im = new IndexManager();
            }
        }
        return im;
    }

    /**
     * Startup or fetches a running ElasticSearch Instance for a given user and returns a client handle for
     * communication
     */
    public synchronized Client initAndCreateAndDoEverthing(Long userId) {
        try {
            //checks if an ES instance is responding and returns a new client instance if so
            this.getESClusterState(userId);

            //keep instance running for another 20 minutes
            IndexKeepAliveTimer.getInstance().extendTTL20(userId);
            //return the client handle
            return this.getESTransportClient(userId.intValue());

        } catch (IndexManagerCoreException e) {
            //in this case we need to fire up an ES instance for this user
            try {
                this.startupInstance(userId.intValue());
                return this.getESTransportClient(userId.intValue());

            } catch (IndexManagerCoreException e1) {
                this.log.error("failed to startup/connect with running instance and return a client object for user "
                        + userId + ". Returning null", e1);

                //in this case return null for now.
                return null;
            }
        }
    }

    // TODO @see ESConfigurationHandler.checkPortRangeAccepted - these values
    // are currently hardcoded there

    private final Logger log = LoggerFactory.getLogger(IndexManager.class);

    private HashMap<URL, AvailableESInstanceState> availableESInstances = new HashMap<>();

    private URL defaultHost = null;

    // @Inject
    // protected Connection conn;

    // @Inject
    protected DataAccessLayer dal;

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private IndexManagerDao dao;
    private IndexCoreGarbageCollector cleanupTask;

    private static volatile IndexManager im;

    // this class is implemented as singleton
    private IndexManager() {
        try {
            createEntityManager();

            initAvailableInstances();
            this.cleanupTask = new IndexCoreGarbageCollector();

        } catch (MalformedURLException | URISyntaxException | UnknownHostException e) {
            this.log.error("IndexManager initialization failed ", e);

        }
    }

    // ========================================================================

    // CDI lifecycle methods --------------------------------------------------
    @PostConstruct
    public void startupIndexManager() {
        this.log.debug("startup() IndexManager (ApplicationScoped) completed");
    }

    @PreDestroy
    public void shutdownIndexManager() {
        this.log.debug("shutdown IndexManager (ApplicationScoped) started");

        //cleanup - shutdown all running instances
        shutdownAllRunningInstances(this.defaultHost);
        this.log.debug("shutdown all running ElasticSearch instances on " + this.defaultHost + " completed");

        shutdownGarbageCollection();

        this.entityManager.close();
        this.entityManagerFactory.close();
    }

    void shutdownGarbageCollection() {
        //stop the garbage collector
        this.cleanupTask.end();
        this.log.debug("ended IndexKeepAliveTimer.");
    }

    // ========================================================================

    private void createEntityManager() {
        // TODO @Inject
        this.entityManagerFactory = Persistence.createEntityManagerFactory("org.backmeup.index.jpa");

        this.dal = new DataAccessLayerImpl();
        this.entityManager = this.entityManagerFactory.createEntityManager();
        this.dal.setEntityManager(this.entityManager);
        this.dao = this.dal.createIndexManagerDao();
    }

    /**
     * For now this information is static and only synced with running records stored within the DB. TODO - add the
     * option for cluster configuration in property file - central connection to all cluster instances - lightweight
     * module to start/stop instances on each cluster instance
     */
    private void initAvailableInstances() throws MalformedURLException, URISyntaxException, UnknownHostException {

        this.defaultHost = new URI("http", InetAddress.getLocalHost().getHostAddress() + "", "", "").toURL();

        List<Integer> supportedTcpPorts = new ArrayList<>();
        List<Integer> supportedHttpPorts = new ArrayList<>();

        // init the available port range on elasticsearch
        // Note: @see ESConfigurationHandler.checkPortRangeAccepted - these
        // values are currently also hardcoded there
        // TODO reset the port range to 9300 and 9200
        for (int i = 9360; i <= 9399; i++) {
            supportedTcpPorts.add(i);
        }
        for (int i = 9260; i <= 9299; i++) {
            supportedHttpPorts.add(i);
        }
        AvailableESInstanceState esInstance1 = new AvailableESInstanceState(supportedTcpPorts, supportedHttpPorts);
        this.availableESInstances.put(this.defaultHost, esInstance1);

        syncAvailablePortswithPortsInUseFromDB(this.defaultHost);
    }

    /**
     * When initializing the manager sync the available port information with the ones already in use - this information
     * is persisted within the DB - if a given instance is not up and running anymore issue shutdown otherwise keep
     * instance running and reconnect
     */
    private void syncAvailablePortswithPortsInUseFromDB(URL host) {

        // get all running instances according to the DB entries
        List<RunningIndexUserConfig> runningConfigs = this.dao.getAllESInstanceConfigs(host);

        // update the list of available ports for this host
        for (RunningIndexUserConfig config : runningConfigs) {
            if ((config.getHostAddress() != null) && (config.getHttpPort() != null))
                if (this.availableESInstances.get(config.getHostAddress()) != null) {
                    //check the instance's state
                    try {
                        //check if the instance is still up and running
                        this.getESClusterState(config.getUserID());
                        // remove host + port from available ones
                        this.availableESInstances.get(config.getHostAddress()).removeAvailableHTTPPort(
                                config.getHttpPort());
                        this.availableESInstances.get(config.getHostAddress()).removeAvailableTCPPort(
                                config.getTcpPort());
                    } catch (IndexManagerCoreException e) {
                        //not reachable - try to clean up the mess
                        this.shutdownInstance(config.getUserID().intValue());
                    }
                }
        }
    }

    /**
     * Runs the ES configuration, index data mounting from TrueCrypt and powers on a ES instance for a given user. mount
     * truecrypt container, create user specific ES launch configuration (yml file), start ES instance for user
     * elasticsearch -Des.config="C:\Program Files\elasticsearch-1.2.0\config\elasticsearch.user0.yml"
     * 
     * @throws NumberFormatException
     *             when the available range of supported ports on ES is used up
     * @throws ExceptionInInitializerError
     *             when issuing the call to TrueCrypt failed
     * @throws IllegalArgumentException
     *             when the TrueCrypt instance was not configured properly
     */
    public synchronized void startupInstance(int userID) throws IndexManagerCoreException {

        this.log.debug("startupInstance for userID: " + userID + " started");

        // 1) check if user has been initialized
        File fTCContainerOnDataSink = null;
        try {
            fTCContainerOnDataSink = ThemisDataSink.getIndexTrueCryptContainer(userID);
        } catch (IOException e) {
            // initialize user
            init(userID);
            // try the call again - user now initialized, if error -> fail
            try {
                fTCContainerOnDataSink = ThemisDataSink.getIndexTrueCryptContainer(userID);
            } catch (IOException e1) {
                String s = "startupInstance for userID: " + userID + " step1 - failed";
                this.log.debug(s, e1);
                throw new IndexManagerCoreException(s, e1);
            }
        }
        this.log.debug("startupInstance for userID: " + userID + " step1 - ok");

        // 2) get a local copy of the TrueCrypt container for the given user
        File fTCContainer;
        try {
            fTCContainer = copyTCContainerFileToLocalWorkingDir(fTCContainerOnDataSink, userID);
            this.log.debug("startupInstance for userID: " + userID + " step2 - ok");
        } catch (IOException e1) {
            String s = "startupInstance for userID: " + userID + " step2 - failed";
            this.log.debug(s, e1);
            throw new IndexManagerCoreException(s, e1);
        }

        // 3) Now mount the ES data volume
        // TODO currently when all available drives are in use the system will throw an IOException
        String tcMountedDriveLetter;

        try {
            tcMountedDriveLetter = TCMountHandler.mount(fTCContainer, "12345", TCMountHandler
                    .getSupportedDriveLetters().get(0));
            this.log.debug("startupInstance for userID: " + userID + " step3 - ok");
            this.log.debug("Mounted Drive Letter: " + tcMountedDriveLetter + "from: " + fTCContainer.getAbsolutePath());
        } catch (ExceptionInInitializerError | IllegalArgumentException | IOException | InterruptedException e1) {
            String s = "startupInstance for userID: " + userID + " step3 - failed";
            this.log.debug(s, e1);
            throw new IndexManagerCoreException(s, e1);
        }

        // 4) crate a user specific ElasticSearch startup configuration file
        // TODO currently when all available ports are in use the system will throw a NumberFormatException
        int tcpPort = getFreeESTCPPort();
        int httpPort = getFreeESHttpPort();

        // this file contains the user specific ES startup config (data, ports, etc.)
        try {
            ESConfigurationHandler.createUserYMLStartupFile(userID, this.defaultHost, tcpPort, httpPort,
                    tcMountedDriveLetter);
            this.log.debug("startupInstance for userID: " + userID + " step4 - ok");
        } catch (NumberFormatException | ExceptionInInitializerError | IOException e1) {
            String s = "startupInstance for userID: " + userID + " step4 - failed";
            this.log.debug(s, e1);
            throw new IndexManagerCoreException(s, e1);
        }

        // 5) persist the configuration within the database
        try {
            // TODO currently only one host machine for ES supported: localhost
            URI uri = new URI("http", InetAddress.getLocalHost().getHostAddress() + "", "", "");

            // keep a database record of this configuration
            RunningIndexUserConfig runningConfig = new RunningIndexUserConfig(Long.valueOf(userID), uri.toURL(),
                    tcpPort, httpPort, "user" + userID, tcMountedDriveLetter, fTCContainer.getAbsolutePath());

            // persist the configuration
            this.entityManager.getTransaction().begin();
            this.dao.save(runningConfig);
            this.entityManager.getTransaction().commit();
            this.log.debug("startupInstance for userID: " + userID + " step5 - ok");

        } catch (Exception e1) {
            String s = "startupInstance for userID: " + userID + " step5 - failed";
            this.log.debug(s, e1);
            throw new IndexManagerCoreException(s, e1);
        }

        // 6) now power on elasticsearch
        try {
            ESConfigurationHandler.startElasticSearch(userID);
            this.log.debug("startupInstance for userID: " + userID + " step6 - ok");
            this.log.info("started ES Instance " + getRunningIndexUserConfig(userID).getClusterName() + " on host: "
                    + getRunningIndexUserConfig(userID).getHostAddress().getHost() + ":"
                    + getRunningIndexUserConfig(userID).getHttpPort());

        } catch (IOException | InterruptedException e1) {
            String s = "startupInstance for userID: " + userID + " step6 - failed";
            this.log.debug(s, e1);
            throw new IndexManagerCoreException(s, e1);
        }

        // 7) check instance up and running
        try {
            getESClusterState(Long.valueOf(userID));
        } catch (IndexManagerCoreException e1) {
            String s = "startupInstance for userID: " + userID + " step7 - failed";
            this.log.debug(s, e1);
            throw new IndexManagerCoreException(s, e1);
        }

        // 8) register a timeout for this instance
        IndexKeepAliveTimer.getInstance().extendTTL20(Long.valueOf(userID));
    }

    /**
     * Handles the shutdown (rollback) of TC, ES, Sharing, DB-persistency, etc. for a given instance
     */
    public synchronized void shutdownInstance(int userID) {
        this.log.debug("shutdownInstance for userID: " + userID + " started");

        //1. get the perstisted records from DB
        RunningIndexUserConfig runningInstanceConfig = getRunningIndexUserConfig(userID);
        if (runningInstanceConfig == null) {
            //if they are null we can't do anything
            this.log.debug("shutdownInstance for userID: " + userID
                    + " step1 - failed, no configuration persisted in db");
            return;
        } else {
            this.log.debug("shutdownInstance for userID: " + userID + " step1 - ok");
        }

        //2. shutdown the ElasticSearch Instance
        try {
            ESConfigurationHandler.stopElasticSearch(userID);
            this.log.debug("shutdownInstance for userID: " + userID + " step2 - ok");
        } catch (IOException e) {
            this.log.debug("shutdownInstance for userID: " + userID + " step2 - failed", e);
        }

        //3. unmount the truecrypt volume
        try {
            String driveLetter = runningInstanceConfig.getMountedTCDriveLetter();
            TCMountHandler.unmount(driveLetter);
            this.log.debug("shutdownInstance for userID: " + userID + " step3 - ok");
        } catch (IllegalArgumentException | ExceptionInInitializerError | IOException | InterruptedException e) {
            this.log.debug("shutdownInstance for userID: " + userID + " step3 - failed", e);
        }

        //4. persist the index data files within the container back to the Themis data sink
        try {
            ThemisDataSink.saveIndexTrueCryptContainer(new File(runningInstanceConfig.getMountedContainerLocation()),
                    userID);
            this.log.debug("shutdownInstance for userID: " + userID + " step4 - ok");
        } catch (IOException e) {
            this.log.debug("shutdownInstance for userID: " + userID + " step4 - failed", e);
        }

        //5. remove the userconfiguration from db and release the ports
        try {
            releaseRunningInstanceMapping(userID);
            this.log.debug("shutdownInstance for userID: " + userID + " step4 - ok");
        } catch (IOException e) {
            this.log.debug("shutdownInstance for userID: " + userID + " step5 - failed", e);
        }

        //6. wipe the temp working directory
        deleteLocalWorkingDir(userID);
        this.log.debug("shutdownInstance for userID: " + userID + " completed ok");

        //7. remove entries in the garbage collector
        IndexKeepAliveTimer.getInstance().flagAsShutdown(Long.valueOf(userID));
    }

    /**
     * Shuts down all running ES + TC instances on a given host
     */
    private void shutdownAllRunningInstances(URL host) {
        // get all running instances according to the DB entries
        List<RunningIndexUserConfig> runningConfigs = this.dao.getAllESInstanceConfigs(host);
        for (RunningIndexUserConfig con : runningConfigs) {
            shutdownInstance(con.getUserID().intValue());
        }
    }

    /**
     * Cleanup - stops all running ES instances, removes all mounted TC container and drops database records of running
     * isntances
     */
    private void cleanupRude() {

        this.log.debug("cleanupRude: started stopping all ES instances");
        // shutdown all elastic search instances
        ESConfigurationHandler.stopAllRude();
        this.log.debug("cleanupRude: completed - no ES instances running");

        // unmount all open TrueCrypt volumes
        try {
            this.log.debug("cleanupRude: started unmounting all TC instances");
            TCMountHandler.unmountAll();
            this.log.debug("cleanupRude: completed - all TC volumes unmounted");
        } catch (IOException | InterruptedException e) {
            this.log.debug("cleanupRude: unmounting all TC volumes failed", e);
        }

        //remove the userconfiguration from db and release the ports
        this.log.debug("cleanupRude: started removing all DB records: executing " + "DELETE FROM +"
                + RunningIndexUserConfig.class.getSimpleName());
        this.entityManager.getTransaction().begin();
        this.entityManager.createQuery("DELETE FROM +" + RunningIndexUserConfig.class.getSimpleName()).executeUpdate();
        this.entityManager.getTransaction().commit();
        this.log.debug("cleanupRude: removing all DB records: completed");

        try {
            this.log.debug("cleanupRude: started reInitializing");
            initAvailableInstances();
            this.log.debug("cleanupRude: completed reInitializing");
        } catch (MalformedURLException | UnknownHostException | URISyntaxException e) {
            this.log.debug("cleanupRude: reInitializing failed", e);
        }

        // TODO delete all working directories?
    }

    public RunningIndexUserConfig getRunningIndexUserConfig(int userID) {
        return this.dao.findConfigByUserId(Long.valueOf(userID));
    }

    private int getFreeESHttpPort() {
        // TODO Loadbalancing between the different host machines
        return this.availableESInstances.get(this.defaultHost).useNextHTTPPort();
    }

    private int getFreeESTCPPort() {
        // TODO Loadbalancing between the different host machines
        return this.availableESInstances.get(this.defaultHost).useNextTCPPort();
    }

    /**
     * Cleans up the available and used port mapping and updates the database This method does not stop running ES and
     * TC instances
     */
    private void releaseRunningInstanceMapping(int userID) throws IOException {

        try {
            this.entityManager.getTransaction().begin();
            RunningIndexUserConfig config = this.dao.findById(Long.valueOf(userID));
            this.availableESInstances.get(this.defaultHost).addAvailableHTTPPort(config.getHttpPort());
            this.availableESInstances.get(this.defaultHost).addAvailableTCPPort(config.getTcpPort());

            this.dao.delete(config);
            this.entityManager.getTransaction().commit();
        } catch (Exception e) {
            this.entityManager.getTransaction().rollback();
            throw new IOException("Rolling back transaction for userID: " + userID
                    + " availableHTTP and availableTCP port declaration out of sync");
        }
    }

    /**
     * Inits a user specific elasticsearch instance i.e. copies the container file and registers it within the
     * themis-datasink
     */
    private void init(int userID) {
        // TODO fix weakness currently all copied TC container files have the
        // same default password as this cannot be changed via TC command line
        // interface. idea: keep default password but encrypt the container file
        try {
            ThemisDataSink.saveIndexTrueCryptContainer(
                    getClass().getClassLoader().getResourceAsStream("elasticsearch_userdata_template_TC_150MB.tc"),
                    userID);
        } catch (IOException e) {
            this.log.debug("IndexManager init ES instance failed for user" + userID + " due to " + e);
        }
    }

    /**
     * Configures and returns a Client to ElasticSearch to interact with for a specific user
     */
    public Client getESTransportClient(int userID) throws IndexManagerCoreException {
        //TODO Keep Clients and last accessed timestamp? 
        RunningIndexUserConfig conf = getRunningIndexUserConfig(userID);
        if (conf != null) {
            Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", conf.getClusterName()).build();

            // now try to connect with the TransportClient - requires the
            // transport.tcp.port for connection
            Client client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(conf
                    .getHostAddress().getHost(), conf.getTcpPort()));
            return client;
        }

        throw new IndexManagerCoreException("Failed to create ES TransportClient for userID: " + userID
                + " due to missing RunningIndexUserConfig");
    }

    /**
     * Retrieves the ClusterState of a mounted ES cluster for a given userID
     * 
     * @throws IndexManagerCoreException
     *             if now instance is available
     */
    public ClusterState getESClusterState(Long userId) throws IndexManagerCoreException {
        //check if we've got a DB record
        RunningIndexUserConfig config = getRunningIndexUserConfig(userId.intValue());

        if (config != null) {
            Client client = this.getESTransportClient(userId.intValue());
            ClusterState clusterState = client.admin().cluster().state(new ClusterStateRequest())
                    .actionGet(10, TimeUnit.SECONDS).getState();
            client.close();
            this.log.debug("Clusterstate for userID: " + userId + " " + clusterState.prettyPrint());
            return clusterState;
        }

        throw new IndexManagerCoreException("Clusterstate for userID: " + userId + " " + "Cluster not responding");
    }

    /**
     * Gets the root directory (for index operations) for all users done.
     */
    private static String getUserDataWorkingDirRoot() {
        String s = Configuration.getProperty("index.temp.data.home.dir");
        if (s != null && s.length() > 0 && !s.contains("\"")) {
            File f = new File(s);
            if (f.isDirectory() && f.exists()) {
                return f.getAbsolutePath();
            }

            f.mkdirs();
            if (f.isDirectory() && f.exists()) {
                return f.getAbsolutePath();
            }

            throw new ExceptionInInitializerError(
                    "index.temp.data.home.dir does not exist or is not accessible to system");
        }
        throw new ExceptionInInitializerError(
                "index.temp.data.home.dir not properly configured within backmeup-indexer.properties");
    }

    /**
     * Gets the user's working directory on the temporary file share to operate the index upon
     */
    public static String getUserDataWorkingDir(int userID) {
        return getUserDataWorkingDirRoot() + "/user" + userID;
    }

    private File copyTCContainerFileToLocalWorkingDir(File f, int userID) throws IOException {
        return FileUtils.copyFileUsingChannel(f, new File(getUserDataWorkingDir(userID)
                + "/index/elasticsearch_userdata_TC_150MB.tc"));
    }

    /**
     * Deletes the working directory for a given user including all files within it
     */
    private void deleteLocalWorkingDir(int userID) {
        File f = new File(getUserDataWorkingDir(userID));
        if (f.exists()) {
            FileUtils.deleteDirectory(f);
        }

    }

    /**
     * required for testing purposes to inject a different db configuration
     */
    public void setEntityManager(EntityManager em) {
        this.entityManager = em;
        this.dal = new DataAccessLayerImpl();
        this.dal.setEntityManager(this.entityManager);
        this.dao = this.dal.createIndexManagerDao();
    }

}
