package org.backmeup.index;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.model.User;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexGarbageCollectionTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexManager indexManager;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;

    @RunRequestScoped
    @Override
    public void run() {
        log.debug("started running garbage collection for ElasticSearch Instances no longer in use.");
        List<User> userIDs = indexKeepAliveTimer.getUsersToShutdown();

        int openInstances = indexKeepAliveTimer.countOpenInstances();
        log.debug("Open ES instances: " + openInstances + " marked instances for shutdown: " + userIDs.size());

        for (User userId : userIDs) {
            log.info("IndexCoreGarbageCollector executing shutdown for userID: " + userId);

            //iterate over all instances to shutdown
            indexManager.shutdownInstance(userId);
        }
    }
}