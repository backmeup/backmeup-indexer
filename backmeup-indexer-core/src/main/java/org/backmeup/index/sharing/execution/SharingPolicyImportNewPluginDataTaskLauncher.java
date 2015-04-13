package org.backmeup.index.sharing.execution;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Periodically checks for no longer required running ElasticSearch instances and issues a shutdown request
 */
@ApplicationScoped
public class SharingPolicyImportNewPluginDataTaskLauncher {

    private int SECONDS_BETWEEN_RECHECKING = 2;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);

    @Inject
    private SharingPolicyImportNewPluginDataTask task;

    public void startupSharingPolicyExecution() {
        this.log.debug("startup SharingPolicyImportNewPluginDataTask (ApplicationScoped) - started");

        this.exec.scheduleAtFixedRate(this.task, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING,
                TimeUnit.SECONDS);
        this.log.debug("startup SharingPolicyImportNewPluginDataTask - completed");
    }

    public void shutdownSharingPolicyExecution() {
        //stopping index-plugin data distribution thread
        this.log.debug("shutdown SharingPolicyImportNewPluginDataTask (ApplicationScoped) - started");
        this.exec.shutdown();
        this.log.debug("shutdown SharingPolicyImportNewPluginDataTask - completed");
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
