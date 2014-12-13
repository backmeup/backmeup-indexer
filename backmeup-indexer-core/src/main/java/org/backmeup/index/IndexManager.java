package org.backmeup.index;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.backmeup.index.core.elasticsearch.SearchInstanceException;
import org.backmeup.index.core.elasticsearch.SearchInstances;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.core.truecrypt.EncryptionProvider;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.dal.jpa.JPADataAccessLayer;
import org.backmeup.index.error.IndexManagerCoreException;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ES;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexManager {

    /**
     * Startup or fetches a running ElasticSearch Instance for a given user and returns a client handle for
     * communication
     */
    public synchronized Client initAndCreateAndDoEverthing(User userId) {
        Client client = startup.renewsExistingInstance(userId);
        if (client != null) {
            return client;
        }

        //in this case we need to fire up an ES instance for this user
        try {
            return startup.startupNewInstance(userId);

        } catch (IndexManagerCoreException e1) {
            // rollback the startup steps that were already performed
            shutdown.shutdownInstance(userId);
            this.log.error("failed to startup/connect with running instance and return a client object for user "
                    + userId + ". Returning null", e1);

            //in this case return null for now.
            throw e1;
        }
    }

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject 
    private EncryptionProvider encryptionProvider;
    @Inject 
    private SearchInstances searchInstance;
    @Inject
    private IndexManagerDao dao;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;
    @Inject 
    private IndexCoreGarbageCollector cleanupTask;
    @Inject 
    private IndexShutdown shutdown;
    @Inject 
    private IndexStartup startup;
    @Inject 
    private ES es;

    @PostConstruct
    public void startupIndexManager() {
        if (cleanupTask!=null) {
            // not in tests, so start it
         
            // add .toString() for eagerly initialising ApplicationScoped Beans
            // need to instantiate in order for the timer to start running
            this.cleanupTask.toString();
        }

        // Initialisation of IndexManager managed ElasticSearch instances
        syncManagerAfterStartupFromDBRecords(searchInstance.getDefaultHost());

        this.log.debug("startup() IndexManager (ApplicationScoped) completed");
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
                if (searchInstance.isKnownHost(config.getHostAddress())) {
                    //check the instance's state
                    try {
                        //check if the instance is still up and running
                        es.getESClusterState(config.getUser());
                        // remove host + port from available ones
                        searchInstance.takeHostPorts(config);
                        //register this instance for GarbageCollection
                        this.indexKeepAliveTimer.extendTTL20(config.getUser());

                        this.log.debug("properly recovered running ElasticSearch instance for "
                                + config.getHostAddress() + " and userID: " + config.getUserID() + " and httpPort: "
                                + config.getHttpPort());

                    } catch (SearchInstanceException e) {
                        this.log.debug("skipping recovery - instance not responding for " + config.getHostAddress()
                                + " and userID: " + config.getUserID() + " and httpPort: " + config.getHttpPort());
                        //not reachable - try to clean up the mess
                        shutdown.shutdownInstance(config);
                    }
                } else {
                    this.log.debug("skipping recovery - " + config.getHostAddress()
                            + "  no longer supported. Deleting RunninInstanceUserConfig for userID: "
                            + config.getUserID());
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
     * Cleanup - stops all running ES instances, removes all mounted TC container and drops database records of running
     * instances
     */
    private void cleanupRude() {
        searchInstance.shutdownAllIndexNodes();

        // unmount all open TrueCrypt volumes
        encryptionProvider.unmountAll();

        //remove the userconfiguration from db and release the ports
        this.log.debug("cleanupRude: started removing all DB records: executing " + "DELETE FROM +"
                + RunningIndexUserConfig.class.getSimpleName());
        this.dao.deleteAll();
        this.log.debug("cleanupRude: removing all DB records: completed");

        try {
            this.log.debug("cleanupRude: started reInitializing");
            searchInstance.initAvailableInstances();
            syncManagerAfterStartupFromDBRecords(searchInstance.getDefaultHost());
            this.log.debug("cleanupRude: completed reInitializing");
        } catch (MalformedURLException | UnknownHostException | URISyntaxException e) {
            this.log.debug("cleanupRude: reInitializing failed", e);
        }

        // TODO delete all working directories?
    }

    /**
     * required for testing purposes to inject a different db configuration
     */
    void injectForTests(EntityManager em) {
        JPADataAccessLayer dal = new JPADataAccessLayer();
        dal.setEntityManager(em);
        this.dao = dal.createIndexManagerDao();
        this.indexKeepAliveTimer = new IndexKeepAliveTimer();
    }

}
