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
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexManager {

    public static IndexManager getInstance() {
        return im;
    }

    public Client initAndCreateAndDoEverthing(Long userId) {
        try {
            // TODO Andrew fix all ID to long
            IndexManager.getInstance().startupInstance(userId.intValue());
            return IndexManager.getInstance().getESTransportClient(userId.intValue());
        } catch (Exception ex) {
            throw new IndexingException(ex);
        }
    };

    // TODO @see ESConfigurationHandler.checkPortRangeAccepted - these values
    // are currently hardcoded there

    private final Logger log = LoggerFactory.getLogger(getClass());

    private HashMap<URL, AvailableESInstanceState> availableESInstances = new HashMap<>();

    private URL defaultHost = null;

    // @Inject
    // protected Connection conn;

    // @Inject
    protected DataAccessLayer dal;

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private IndexManagerDao dao;

    private static IndexManager im = new IndexManager();

    // this class is implemented as singleton
    private IndexManager() {
        try {
            createEntityManager();
            initAvailableInstances();

        } catch (MalformedURLException | URISyntaxException | UnknownHostException e) {
            this.log.error("IndexManager initialization failed " + e);

        }
    }

    // ========================================================================

    // CDI lifecycle methods --------------------------------------------------
    @PostConstruct
    public void startupIndexManager() {
        this.log.debug("startup() IndexManager (ApplicationScoped)");
    }

    @PreDestroy
    public void shutdownIndexManager() {
        this.log.debug("shutdown() IndexManager (ApplicationScoped)");
        this.entityManagerFactory.close();
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
        // TODO reset the port range
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
     * is persisted within the DB
     */
    private void syncAvailablePortswithPortsInUseFromDB(URL host) {

        // get all running instances according to the DB entries
        List<RunningIndexUserConfig> runningConfigs = this.dao.getAllESInstanceConfigs(host);

        // update the list of available ports for this host
        for (RunningIndexUserConfig config : runningConfigs) {
            if ((config.getHostAddress() != null) && (config.getHttpPort() != null))
                if (this.availableESInstances.get(config.getHostAddress()) != null) {
                    // remove host + port from available instances
                    this.availableESInstances.get(config.getHostAddress())
                            .removeAvailableHTTPPort(config.getHttpPort());
                    this.availableESInstances.get(config.getHostAddress()).removeAvailableTCPPort(config.getTcpPort());
                }
        }
    }

    // TODO Add logging statements for warnings and errors
    // TODO encrypt ES Webservice Endpoint (certificates or basic
    // authentication

    /**
     * Runs the ES configuration, index data mounting from TrueCrypt and powers on a ES instance for a given user. mount
     * truecrypt container, create user specific ES launch configuration (yml file), start ES instance for user
     * elasticsearch -Des.config="C:\Program Files\elasticsearch-1.2.0\config\elasticsearch.user0.yml"
     * 
     * @throws IOException
     *             when the required artifact files are not properly created
     * @throws NumberFormatException
     *             when the available range of supported ports on ES is used up
     * @throws ExceptionInInitializerError
     *             when issuing the call to TrueCrypt failed
     * @throws InterruptedException
     *             when issuing the call to TrueCrypt failed
     * @throws IllegalArgumentException
     *             when the TrueCrypt instance was not configured properly
     */
    public void startupInstance(int userID) throws IOException, NumberFormatException, InterruptedException {

        // 1) check if user has been initialized
        File fTCContainerOnDataSink = null;
        try {
            fTCContainerOnDataSink = ThemisDataSink.getIndexTrueCryptContainer(userID);
        } catch (IOException e) {
            // initialize user
            init(userID);
            // try the call again - user now initialized, if error -> throw
            // exceptions
            fTCContainerOnDataSink = ThemisDataSink.getIndexTrueCryptContainer(userID);
        }

        // 2) get a local copy of the TrueCrypt container for the given user
        File fTCContainer = copyTCContainerFileToLocalWorkingDir(fTCContainerOnDataSink, userID);

        // 3) Now mount the ES data volume
        // TODO currently when all available drives are in use the system will
        // throw an IOException
        String tcMountedDriveLetter;

        tcMountedDriveLetter = TCMountHandler.mount(fTCContainer, "12345", TCMountHandler.getSupportedDriveLetters()
                .get(0));

        this.log.debug("Mounted Drive Letter: " + tcMountedDriveLetter + "from: " + fTCContainer.getAbsolutePath());

        // 4) crate a user specific ElasticSearch startup configuration file
        // TODO currently when all available ports are in use the system will
        // throw a NumberFormatException
        int tcpPort = getFreeESTCPPort();
        int httpPort = getFreeESHttpPort();

        // this file contains the user specific port configuration, data and log
        // location, etc.
        ESConfigurationHandler.createUserYMLStartupFile(userID, this.defaultHost, tcpPort, httpPort,
                tcMountedDriveLetter);

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

        } catch (URISyntaxException e) {
            // may not happen
            this.entityManager.getTransaction().rollback();
        }

        // just of testing:
        RunningIndexUserConfig c = this.dao.findConfigByUserId(Long.valueOf(userID));
        this.log.debug("using mounting point: " + c.getMountedTCDriveLetter() + " for source: "
                + c.getMountedContainerLocation());

        // 5) now power on elasticsearch
        ESConfigurationHandler.startElasticSearch(userID);
        this.log.debug("started ES Instance? on host: " + getRunningIndexUserConfig(userID).getClusterName() + ":"
                + getRunningIndexUserConfig(userID).getHttpPort());
        // check instance up and running
        // import waiting index files (shared data)
    }

    public void shutdownInstance(int userID) throws IllegalArgumentException, ExceptionInInitializerError, IOException,
            InterruptedException {
        // TODO need to create a transaction for this

        RunningIndexUserConfig runningInstanceConfig = getRunningIndexUserConfig(userID);

        // shutdown the ElasticSearch Instance
        ESConfigurationHandler.stopElasticSearch(userID);

        // unmount the truecrypt volume
        String driveLetter = runningInstanceConfig.getMountedTCDriveLetter();
        TCMountHandler.unmount(driveLetter);

        // persist the index data files within the container back to the Themis
        // data sink
        ThemisDataSink.saveIndexTrueCryptContainer(new File(runningInstanceConfig.getMountedContainerLocation()),
                userID);

        // remove the userconfiguration from db and release the ports
        releaseRunningInstanceMapping(userID);

        // whipe the temp working directory
        deleteLocalWorkingDir(userID);
    }

    /**
     * Cleanup - stops all running ES instances, removes all mounted TC container - NOT RECOMMENDED
     */
    public void cleanupRude() throws IOException, InterruptedException {
        // TODO Implement as Admin method
        // unmount all open TrueCrypt volumes
        TCMountHandler.unmountAll();
        // shutdown all elastic search instances
        ESConfigurationHandler.stopAll();
        // TODO delete all working directories
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
    private void releaseRunningInstanceMapping(int userID) {

        this.entityManager.getTransaction().begin();
        RunningIndexUserConfig config = this.dao.findById(Long.valueOf(userID));
        this.availableESInstances.get(this.defaultHost).addAvailableHTTPPort(config.getHttpPort());
        this.availableESInstances.get(this.defaultHost).addAvailableTCPPort(config.getTcpPort());

        this.dao.delete(config);
        this.entityManager.getTransaction().commit();
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
            ThemisDataSink.saveIndexTrueCryptContainer(getClass().getClassLoader().getResourceAsStream(
                    "elasticsearch_userdata_template_TC_150MB.tc"), userID);
        } catch (IOException e) {
            this.log.debug("IndexManager init ES instance failed for user" + userID + " due to " + e);
        }
    }

    /**
     * Configures and returns a Client to ElasticSearch to interact with for a specific user
     * 
     * @param userID
     * @return
     */
    public Client getESTransportClient(int userID) {
        RunningIndexUserConfig conf = getRunningIndexUserConfig(userID);
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "user" + userID).build();

        // now try to connect with the TransportClient - requires the
        // transport.tcp.port for connection
        Client client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress(conf
                .getHostAddress().getHost(), conf.getTcpPort()));
        return client;
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
