package org.backmeup.index;

import java.io.File;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.core.datacontainer.UserDataStorage;
import org.backmeup.index.core.elasticsearch.SearchInstances;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.core.truecrypt.EncryptionProvider;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.error.IndexManagerCoreException;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ES;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexStartup {

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
    private ES es;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;
    
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
    public Client startupNewInstance(User userID) throws IndexManagerCoreException {

        this.log.debug("startupInstance for userID: " + userID + " started");

        // 1) check if user has been initialized
        File fTCContainerOnDataSink = dataContainer.getUserStorageCryptContainerFor(userID);
        this.log.debug("startupInstance for userID: " + userID + " step1 - ok");

        // 2) get a local copy of the TrueCrypt container for the given user
        File fTCContainer = dataContainer.copyUserStorageCryptContainerToLocalWorkingDir(userID, fTCContainerOnDataSink);
        this.log.debug("startupInstance for userID: " + userID + " step2 - ok");

        // 3) Now mount the ES data volume
        String tcMountedDriveLetter = encryptionProvider.mountNextFreeMountPoint(userID, fTCContainer);
        this.log.debug("startupInstance for userID: " + userID + " step3 - ok");

        // 4) crate a user specific ElasticSearch startup configuration file
        RunningIndexUserConfig runningConfig = searchInstance.createIndexUserConfig(userID, fTCContainer, tcMountedDriveLetter);

        // this file contains the user specific ES startup config (data, ports, etc.)
        searchInstance.createIndexStartFile(runningConfig);

        // 5) persist the configuration within the database
        runningConfig = this.dao.save(runningConfig);
        this.log.debug("startupInstance for userID: " + userID + " step5 - ok");

        // 6) now power on elasticsearch
        searchInstance.startIndexNode(runningConfig);

        // 7) check instance up and running
        Client client = es.getESTransportClient(runningConfig);
        
        int maxAttempts = 3;
        int sleepSeconds = 2;
        es.getESClusterState(runningConfig.getUser(), client, maxAttempts, sleepSeconds);

        //keep instance running for another 20 minutes
        this.indexKeepAliveTimer.extendTTL20(userID);

        return client;
    }

    public Client renewsExistingInstance(User userId) {
        RunningIndexUserConfig conf = dao.findConfigByUser(userId);
        if (conf != null) {
            //checks if an ES instance is responding and returns a new client instance if so
            Client client = es.getESTransportClient(conf);
            
            // sanity check cluster state
            es.getESClusterState(userId, client);
            // TODO this can cause exception, then we would need to shutdown the running/broken instance somehow
    
            //keep instance running for another 20 minutes
            this.indexKeepAliveTimer.extendTTL20(userId);
            
            //return the client handle
            return client;
        }
        return null;
    }
        
    
   
}
