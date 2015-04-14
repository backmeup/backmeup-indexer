package org.backmeup.index.sharing.execution;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches a Thread for executing SharingPolicyUpToDateCheckerTask at a fixed rate
 */
@ApplicationScoped
public class SharingPolicyUpToDateCheckerTaskLauncher {

    private int SECONDS_BETWEEN_RECHECKING = 60;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

    @Inject
    private SharingPolicyUpToDateCheckerTask task;

    public void startupPolicyUpToDateChecker() {
        this.log.debug("startup SharingPolicyUpToDateCheckerTask (ApplicationScoped) - started");

        this.exec.scheduleAtFixedRate(this.task, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING,
                TimeUnit.SECONDS);
        this.log.debug("startup SharingPolicyUpToDateCheckerTask - completed");
    }

    public void shutdownPolicyUpToDateChecker() {
        //stopping index-plugin data distribution thread
        this.log.debug("shutdown SharingPolicyUpToDateCheckerTask (ApplicationScoped) - started");
        this.exec.shutdown();
        this.log.debug("shutdown SharingPolicyUpToDateCheckerTask - completed");
    }

    /**
     * Used for JUnit Tests to modify the default value
     * 
     * @param seconds
     */
    protected void setFrequency(int seconds) {
        this.SECONDS_BETWEEN_RECHECKING = seconds;
    }

}
