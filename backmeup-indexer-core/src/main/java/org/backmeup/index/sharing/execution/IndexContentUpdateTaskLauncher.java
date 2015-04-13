package org.backmeup.index.sharing.execution;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches a Thread for executing IndexContentUpdateTask at a fixed rate
 */
@ApplicationScoped
public class IndexContentUpdateTaskLauncher {

    private int SECONDS_BETWEEN_RECHECKING = 30;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

    @Inject
    private IndexContentUpdateTask task;

    public void startupIndexContentUpdateExecution() {
        this.log.debug("startup IndexContentUpdateTask (ApplicationScoped) - started");
        this.exec.scheduleAtFixedRate(this.task, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING,
                TimeUnit.SECONDS);
        this.log.debug("startup IndexContentUpdateTask - completed");
    }

    public void shutdownIndexContentUpdateExecution() {
        //stopping index-plugin data distribution thread
        this.log.debug("shutdown IndexContentUpdateTask (ApplicationScoped) - started");
        this.exec.shutdown();
        this.log.debug("shutdown IndexContentUpdateTask - completed");
    }

    /**
     * Used for JUnit Tests to modify the default value
     * 
     * @param seconds
     */
    public void setFrequency(int seconds) {
        this.SECONDS_BETWEEN_RECHECKING = seconds;
    }

}
