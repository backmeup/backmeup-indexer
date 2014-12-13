package org.backmeup.index;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.dal.Transaction;
import org.backmeup.index.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class IndexGarbageCollectionTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexShutdown indexShutdown;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;

    // needs its own transaction because this is a background thread
    @Inject
    private Transaction transaction;

    @Override
    public void run() {
        transaction.inside(new Runnable() {
            @Override
            public void run() {
                runWithTransaction();
            }
        });
    }

    private void runWithTransaction() {
        log.debug("started running garbage collection for ElasticSearch Instances no longer in use.");
        List<User> userIDs = indexKeepAliveTimer.getUsersToShutdown();

        int openInstances = indexKeepAliveTimer.countOpenInstances();
        log.debug("Open ES instances: " + openInstances + " marked instances for shutdown: " + userIDs.size());

        for (User userId : userIDs) {
            log.info("IndexCoreGarbageCollector executing shutdown for userID: " + userId);

            //iterate over all instances to shutdown
            indexShutdown.shutdownInstance(userId);
        }
    }
}