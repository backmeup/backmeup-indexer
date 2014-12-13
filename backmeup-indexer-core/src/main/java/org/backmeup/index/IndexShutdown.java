package org.backmeup.index;

import java.net.URL;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.core.datacontainer.UserDataStorage;
import org.backmeup.index.core.elasticsearch.SearchInstances;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.core.truecrypt.EncryptionProvider;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexShutdown {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject 
    private UserDataStorage dataContainer;
    @Inject 
    private EncryptionProvider encryptionProvider;
    @Inject 
    private SearchInstances searchInstance;
    @Inject
    private IndexManagerDao dao;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;

    // TODO PK needs db and transaction in another thread
    @PreDestroy
    public void shutdownIndexManager() {
        this.log.debug("shutdown IndexManager (ApplicationScoped) started");

        //cleanup - shutdown all running instances
        shutdownAllRunningInstances(searchInstance.getDefaultHost());
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
     * Handles the shutdown (rollback) of TC, ES, Sharing, DB-persistency, etc. for a given instance
     */
    public void shutdownInstance(User userID) {
        this.log.debug("shutdownInstance for userID: " + userID + " started");

        //1. get the perstisted records from DB
        RunningIndexUserConfig runningInstanceConfig = this.dao.findConfigByUser(userID);
        if (runningInstanceConfig == null) {
            //if they are null we can't do anything
            this.log.debug("shutdownInstance for userID: " + userID
                    + " step1 - failed, no configuration persisted in db");
            return;
        }
        shutdownInstance(runningInstanceConfig);
    }

    public void shutdownInstance(RunningIndexUserConfig config) {
        User userID = config.getUser();

        this.log.debug("shutdownInstance for userID: " + userID + " step1 - ok");

        //2. shutdown the ElasticSearch Instance
        searchInstance.shutdownIndexNode(config);

        //3. unmount the truecrypt volume
        encryptionProvider.unmount(config);

        //4. persist the index data files within the container back to the Themis data sink
        dataContainer.copyCryptContainerDataBackIntoUserStorage(config);

        //5. remove the userconfiguration from db and release the ports
        searchInstance.releaseHostPorts(config);
        
        this.dao.delete(config);
        this.log.debug("shutdownInstance for userID: " + userID + " step4 - ok");

        //6. wipe the temp working directory
        UserDataWorkingDir.deleteLocalWorkingDir(userID);
        this.log.debug("shutdownInstance for userID: " + userID + " completed ok");

        //7. remove entries in the garbage collector
        this.indexKeepAliveTimer.flagAsShutdown(userID);
    }
    
}
