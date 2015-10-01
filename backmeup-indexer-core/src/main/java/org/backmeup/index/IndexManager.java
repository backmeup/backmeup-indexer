package org.backmeup.index;

import java.io.File;
import java.net.URL;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.core.datacontainer.UserDataStorage;
import org.backmeup.index.core.elasticsearch.SearchInstanceException;
import org.backmeup.index.core.elasticsearch.SearchInstances;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.core.truecrypt.EncryptionProvider;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ES;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexManager {

    /**
     * Startup or fetches a running ElasticSearch Instance for a given user and returns a client handle for
     * communication
     */
    public synchronized Client initAndCreateAndDoEverthing(User user) {
        RunningIndexUserConfig conf = getRunningIndexUserConfig(user);
        try {
            if (conf != null) {
                //checks if an ES instance is responding and returns a new client instance if so

                Client client = this.es.getESTransportClient(conf);

                // sanity check cluster state
                this.es.getESClusterState(user, client, 1, 2);

                // update the keyserver's user authentication token if it was sent along
                updateKeyserverAuthenticationToken(user, conf);

                //keep instance running for another 20 minutes
                this.indexKeepAliveTimer.extendTTL20(user);

                //return the client handle
                return client;
            }
        } catch (SearchInstanceException ex) {
            this.shutdownInstance(user);
        }

        //in this case we need to fire up an ES instance for this user
        try {
            return this.startupInstance(user);

        } catch (Exception e1) {
            // rollback the startup steps that were already performed
            this.shutdownInstance(user);
            this.log.error("failed to startup/connect with running instance and return a client object for user " + user
                    + ". Returning null", e1);

            //in this case return null for now.
            throw e1;
        }
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private ES es;
    @Inject
    private UserDataStorage dataContainer;
    @Inject
    private EncryptionProvider encryptionProvider;
    @Inject
    private SearchInstances searchInstance;
    @Inject
    private RunningIndexUserConfigDao dao;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;

    @RunRequestScoped
    public void startupIndexManager() {
        // Initialisation of IndexManager managed ElasticSearch instances
        syncManagerAfterStartupFromDBRecords(this.searchInstance.getDefaultHost());

        this.log.debug("startup() IndexManager (ApplicationScoped) completed");
    }

    @RunRequestScoped
    public void shutdownIndexManager() {
        this.log.debug("shutdown IndexManager (ApplicationScoped) started");

        //cleanup - shutdown all running instances
        shutdownAllRunningInstances(this.searchInstance.getDefaultHost());
    }

    // ========================================================================

    /**
     * When initializing the manager sync the available port information with the ones already in use - this information
     * is persisted within the DB - if a given instance is not up and running anymore issue shutdown otherwise keep
     * instance running and reconnect
     */
    private void syncManagerAfterStartupFromDBRecords(URL host) {

        this.log.debug("syncManagerAfterStartupFromDBRecords for host: " + host);

        // get all running instances according to the DB entries
        List<RunningIndexUserConfig> runningConfigs = this.dao.getAllESInstanceConfigs(host);
        this.log.debug("found " + runningConfigs.size()
                + " running index configuration records from DB - check for each if ElasticSearch is still active");

        // update the list of available ports for this host
        for (RunningIndexUserConfig config : runningConfigs) {
            if ((config.getHostAddress() != null) && (config.getHttpPort() != null)) {
                if (this.searchInstance.isKnownHost(config.getHostAddress())) {
                    //check the instance's state
                    try {
                        //check if the instance is still up and running
                        getESClusterState(config.getUser());
                        // remove host + port from available ones
                        this.searchInstance.takeHostPorts(config);
                        //register this instance for GarbageCollection
                        this.indexKeepAliveTimer.extendTTL20(config.getUser());

                        this.log.debug("properly recovered running ElasticSearch instance for " + config.getHostAddress() + " and userID: "
                                + config.getUserID() + " and httpPort: " + config.getHttpPort());

                    } catch (SearchInstanceException e) {
                        this.log.debug("skipping recovery - instance not responding for " + config.getHostAddress() + " and userID: "
                                + config.getUserID() + " and httpPort: " + config.getHttpPort());
                        //not reachable - try to clean up the mess
                        this.shutdownInstance(config.getUser());
                    }
                } else {
                    this.log.debug("skipping recovery - " + config.getHostAddress()
                            + "  no longer supported. Deleting RunninInstanceUserConfig for userID: " + config.getUserID());
                    this.dao.delete(config);
                }
            } else {
                this.log.debug("skipping recovery - due to malformed DB record. Deleting RunninInstanceUserConfig for userID: "
                        + config.getUserID());
                this.dao.delete(config);
            }
        }
    }

    /**
     * Runs the ES configuration, index data mounting from TrueCrypt and powers on a ES instance for a given user. mount
     * truecrypt container, create user specific ES launch configuration (yml file), start ES instance for user
     * elasticsearch -Des.config="C:\Program Files\elasticsearch-1.2.0\config\elasticsearch.user0.yml"
     * 
     * @throws SearchInstanceException
     */
    synchronized Client startupInstance(User userID) {

        this.log.debug("startupInstance for userID: " + userID + " started");

        // 1) check if user has been initialized
        File fTCContainerOnDataSink = this.dataContainer.getUserStorageCryptContainerFor(userID);
        this.log.debug("startupInstance for userID: " + userID + " step1 - ok");

        // 2) get a local copy of the TrueCrypt container for the given user
        File fTCContainer = this.dataContainer.copyUserStorageCryptContainerToLocalWorkingDir(userID, fTCContainerOnDataSink);
        this.log.debug("startupInstance for userID: " + userID + " step2 - ok");

        // 3) Now mount the ES data volume
        String tcMountedDriveLetter = this.encryptionProvider.mountNextFreeMountPoint(userID, fTCContainer);
        this.log.debug("startupInstance for userID: " + userID + " step3 - ok");

        // 4) crate a user specific ElasticSearch startup configuration file
        RunningIndexUserConfig runningConfig = this.searchInstance.createIndexUserConfig(userID, fTCContainer, tcMountedDriveLetter);

        // this file contains the user specific ES startup config (data, ports, etc.)
        this.searchInstance.createIndexStartFile(runningConfig);

        // 5) persist the configuration within the database
        runningConfig = this.dao.save(runningConfig);
        this.log.debug("startupInstance for userID: " + userID + " step5 - ok");

        // 6) now power on elasticsearch
        int pid = this.searchInstance.startIndexNode(runningConfig);
        if (pid != -1) {
            //set the Linux/Windows PID of the ElasticSearch process
            runningConfig.setEsPID(pid);
            runningConfig = this.dao.merge(runningConfig);
        }
        this.log.debug("startupInstance for userID: " + userID + " step6 - ok");

        // 7) check instance up and running
        Client client = this.es.getESTransportClient(runningConfig);

        int maxAttempts = 3;
        int sleepSeconds = 2;
        this.es.getESClusterState(userID, client, maxAttempts, sleepSeconds);
        this.log.debug("startupInstance for userID: " + userID + " step7 - ok");

        //keep instance running for another 20 minutes
        this.indexKeepAliveTimer.extendTTL20(userID);

        this.log.debug("startupInstance for userID: " + userID + " completed ok");

        return client;
    }

    /**
     * Handles the shutdown (rollback) of TC, ES, Sharing, DB-persistency, etc. for a given instance
     */
    public synchronized void shutdownInstance(User userID) {
        this.log.debug("shutdownInstance for userID: " + userID + " started");

        //1. get the perstisted records from DB
        RunningIndexUserConfig runningInstanceConfig = getRunningIndexUserConfig(userID);
        if (runningInstanceConfig == null) {
            //if they are null we can't do anything
            this.log.debug("shutdownInstance for userID: " + userID + " step1 - failed, no configuration persisted in db");
            return;
        }
        shutdownInstance(runningInstanceConfig);
    }

    private synchronized void shutdownInstance(RunningIndexUserConfig config) {
        User userID = config.getUser();

        this.log.debug("shutdownInstance for userID: " + userID + " step1 - ok");

        //2. shutdown the ElasticSearch Instance
        this.searchInstance.shutdownIndexNode(config);

        //3. unmount the truecrypt volume
        this.encryptionProvider.unmount(config);

        //4. persist the index data files within the container back to the Themis data sink
        this.dataContainer.copyCryptContainerDataBackIntoUserStorage(config);

        //5. remove the userconfiguration from db and release the ports
        this.searchInstance.releaseHostPorts(config);

        this.dao.delete(config);
        this.log.debug("shutdownInstance for userID: " + userID + " step4 - ok");

        //6. wipe the temp working directory
        UserDataWorkingDir.deleteLocalWorkingDir(userID);
        this.log.debug("shutdownInstance for userID: " + userID + " completed ok");

        //7. remove entries in the garbage collector
        this.indexKeepAliveTimer.flagAsShutdown(userID);
    }

    /**
     * Shuts down all running ES + TC instances on a given host
     */
    private void shutdownAllRunningInstances(URL host) {
        // get all running instances according to the DB entries
        List<RunningIndexUserConfig> runningConfigs = this.dao.getAllESInstanceConfigs(host);
        for (RunningIndexUserConfig con : runningConfigs) {
            shutdownInstance(con);
        }
        this.log.debug("shutdown all running ElasticSearch instances on " + host + " completed");
    }

    /**
     * Cleanup - stops all running ES instances, removes all mounted TC container and drops database records of running
     * instances
     */
    private void cleanupRude() {
        this.searchInstance.shutdownAllIndexNodes();

        // unmount all open TrueCrypt volumes
        this.encryptionProvider.unmountAll();

        //remove the userconfiguration from db and release the ports
        this.log.debug("cleanupRude: started removing all DB records: executing " + "DELETE FROM +"
                + RunningIndexUserConfig.class.getSimpleName());
        this.dao.deleteAll();
        this.log.debug("cleanupRude: removing all DB records: completed");

        this.log.debug("cleanupRude: started reInitializing");
        this.searchInstance.initAvailableInstances();
        syncManagerAfterStartupFromDBRecords(this.searchInstance.getDefaultHost());
        this.log.debug("cleanupRude: completed reInitializing");
    }

    private RunningIndexUserConfig getRunningIndexUserConfig(User userID) {
        return this.dao.findConfigByUser(userID);
    }

    /**
     * Configures and returns a Client to ElasticSearch to interact with for a specific user
     */
    public Client getESTransportClient(User userID) throws SearchInstanceException {
        RunningIndexUserConfig conf = getRunningIndexUserConfig(userID);
        return this.es.getESTransportClient(conf);
    }

    /**
     * Retrieves the ClusterState of a mounted ES cluster for a given userID
     * 
     * @throws SearchInstanceException
     *             if now instance is available
     */
    public ClusterState getESClusterState(User userId) throws SearchInstanceException {
        RunningIndexUserConfig conf = getRunningIndexUserConfig(userId);

        try (Client client = this.es.getESTransportClient(conf)) {
            return this.es.getESClusterState(userId, client, 3, 2);
        } catch (SearchInstanceException e) {
            this.log.debug("Get ES cluster state for userID: " + userId + " threw exception: " + e.toString());
            throw new SearchInstanceException("Clusterstate for userID: " + userId + " " + "Cluster not responding");
        }
    }

    private void updateKeyserverAuthenticationToken(User user, RunningIndexUserConfig conf) {
        // update the keyserver's user authentication token if it was provided
        if (user.getKeyServerInternalToken() != null) {
            conf.setKeyServerUserAuthenticationToken(user.getKeyServerInternalToken());
            conf = this.dao.save(conf);
        }
    }

}
