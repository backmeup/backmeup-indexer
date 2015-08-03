package org.backmeup.index.sharing.execution;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Launches a Thread for executing SharingPolicyLifespanCheckerTask at a fixed rate
 * 
 */
@ApplicationScoped
public class SharingPolicyLifespanCheckerTaskLauncher {

    private int SECONDS_BETWEEN_RECHECKING = 20; //TODO SET TO 120

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

    @Inject
    private SharingPolicyLifespanCheckerTask task;

    public void startupPolicyLifespanChecker() {
        this.log.debug("startup SharingPolicyLifespanCheckerTask (ApplicationScoped) - started");

        this.exec.scheduleAtFixedRate(this.task, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING,
                TimeUnit.SECONDS);
        this.log.debug("startup SharingPolicyLifespanCheckerTask - completed");
    }

    public void shutdownPolicyLifespanChecker() {
        this.log.debug("shutdown SharingPolicyLifespanCheckerTask (ApplicationScoped) - started");
        this.exec.shutdown();
        this.log.debug("shutdown SharingPolicyLifespanCheckerTask - completed");
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
