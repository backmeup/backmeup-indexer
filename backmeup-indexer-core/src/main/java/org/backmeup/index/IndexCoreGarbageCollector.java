package org.backmeup.index;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically checks for no longer required running ElasticSearch instances and issues a shutdown request
 */
@ApplicationScoped
public class IndexCoreGarbageCollector {

    private static final int MINUTES_BETWEEN_GBCOLLECTION = 1;

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @Inject
    private IndexGarbageCollectionTask cleanup;

    public void init() {
        this.log.debug("startup garbage collector timer (ApplicationScoped) - started");

        this.scheduler.scheduleAtFixedRate(cleanup, MINUTES_BETWEEN_GBCOLLECTION,
                MINUTES_BETWEEN_GBCOLLECTION, MINUTES);
        this.log.debug("startup garbage collector timer (ApplicationScoped) - completed");
    }
    
    public void end() {
        this.log.debug("shutdown garbage collector timer (ApplicationScoped) - started");
        //stop the garbage collector
        this.scheduler.shutdown();
        this.log.debug("shutdown garbage collector timer (ApplicationScoped) - completed");
    }

}
