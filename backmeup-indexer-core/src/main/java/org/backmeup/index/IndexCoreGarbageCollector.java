package org.backmeup.index;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically checks for no longer required running ElasticSearch instances and issues a shutdown request
 *
 */
public class IndexCoreGarbageCollector {

    private final Logger log = LoggerFactory.getLogger(IndexCoreGarbageCollector.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public IndexCoreGarbageCollector() {
        init();
    }

    public void init() {
        final Runnable cleanup = new Runnable() {
            @Override
            public void run() {
                IndexCoreGarbageCollector.this.log
                        .debug("started running garbage collection for ElasticSearch Instances no longer in use");
                List<Long> userIDs = IndexKeepAliveTimer.getInstance().getUsersToShutdown();
                IndexCoreGarbageCollector.this.log.debug("found " + userIDs.size() + " instances to shutdown");
                for (Long userId : userIDs) {
                    //iterate over all instances to shutdown
                    IndexManager.getInstance().shutdownInstance(userId.intValue());
                    //flag them as done within the timer
                    IndexKeepAliveTimer.getInstance().flagAsShutdown(userId);
                }
            }
        };

        final ScheduledFuture<?> handle = this.scheduler.scheduleAtFixedRate(cleanup, 10, 10, MINUTES);
    }

    public void end() {
        this.scheduler.shutdown();
    }

}
