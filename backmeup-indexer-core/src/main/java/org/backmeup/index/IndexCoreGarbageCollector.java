package org.backmeup.index;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically checks for no longer required running ElasticSearch instances and issues a shutdown request
 */
@ApplicationScoped
public class IndexCoreGarbageCollector {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @Inject
    private IndexManager indexManager;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;

    private final int MINUTES_BETWEEN_GBCOLLECTION = 7;

    @PostConstruct
    public void init() {
        this.log.debug("startup garbage collector timer (ApplicationScoped) - started");
        final Runnable cleanup = new Runnable() {
            @Override
            public void run() {
                IndexCoreGarbageCollector.this.log
                        .debug("started running garbage collection for ElasticSearch Instances no longer in use. intervall= "
                                + IndexCoreGarbageCollector.this.MINUTES_BETWEEN_GBCOLLECTION + " minutes");
                List<Long> userIDs = IndexCoreGarbageCollector.this.indexKeepAliveTimer.getUsersToShutdown();
                int openInstances = IndexCoreGarbageCollector.this.indexKeepAliveTimer.countOpenInstances();
                IndexCoreGarbageCollector.this.log.debug("Open ES instances: " + openInstances
                        + " marked instances for shutdown: " + userIDs.size());

                for (Long userId : userIDs) {
                    IndexCoreGarbageCollector.this.log.info("IndexCoreGarbageCollector executing shutdown for userID: "
                            + userId);
                    //iterate over all instances to shutdown
                    IndexCoreGarbageCollector.this.indexManager.shutdownInstance(userId.intValue());
                    //flag them as done within the timer
                    IndexCoreGarbageCollector.this.indexKeepAliveTimer.flagAsShutdown(userId);
                }
            }
        };

        this.scheduler.scheduleAtFixedRate(cleanup, this.MINUTES_BETWEEN_GBCOLLECTION,
                this.MINUTES_BETWEEN_GBCOLLECTION, MINUTES);
        this.log.debug("startup garbage collector timer (ApplicationScoped) - completed");

    }

    @PreDestroy
    public void end() {
        this.log.debug("shutdown garbage collector timer (ApplicationScoped) - started");
        //stop the garbage collector
        this.scheduler.shutdown();
        this.log.debug("shutdown garbage collector timer (ApplicationScoped) - completed");
    }

    private String getFormatedDate(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
        return sdf.format(d);
    }

}
