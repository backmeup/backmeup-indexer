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
 * Periodically checks for no longer required running ElasticSearch instances
 * and issues a shutdown request
 */
@ApplicationScoped
public class IndexCoreGarbageCollector {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    @Inject
    private IndexManager indexManager;
    @Inject
    private IndexKeepAliveTimer indexKeepAliveTimer;

    @PostConstruct
    public void init() {
        final Runnable cleanup = new Runnable() {
            @Override
            public void run() {
                log.debug("started running garbage collection for ElasticSearch Instances no longer in use at "
                        + getFormatedDate(new Date(System.currentTimeMillis())));
                List<Long> userIDs = indexKeepAliveTimer.getUsersToShutdown();
                log.debug("found " + userIDs.size() + " instances to shutdown");

                for (Long userId : userIDs) {
                    log.info("IndexCoreGarbageCollector executing shutdown for userID: " + userId);
                    //iterate over all instances to shutdown
                    indexManager.shutdownInstance(userId.intValue());
                    //flag them as done within the timer
                    indexKeepAliveTimer.flagAsShutdown(userId);
                }
            }
        };

        this.scheduler.scheduleAtFixedRate(cleanup, 10, 10, MINUTES);
    }

    @PreDestroy
    public void end() {
        //stop the garbage collector
        this.scheduler.shutdown();
        log.debug("shutdown garbage collector timer");
    }

    private String getFormatedDate(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
        return sdf.format(d);
    }

}
