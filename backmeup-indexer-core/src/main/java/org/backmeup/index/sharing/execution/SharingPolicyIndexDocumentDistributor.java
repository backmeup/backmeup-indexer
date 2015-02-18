package org.backmeup.index.sharing.execution;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches the IndexDocuments from the queue and takes care of their distribution into the user's drop-off-for-inport
 * zones according to the defined SharingPolicies
 *
 */
@ApplicationScoped
public class SharingPolicyIndexDocumentDistributor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private int SECONDS_BETWEEN_RECHECKING = 2;

    @Inject
    private IndexDocumentDropOffQueue queue;

    @RunRequestScoped
    public void startupSharingPolicyDistribution() {
        startDistribution();
        this.log.debug("startup() SharingPolicyDistribution (ApplicationScoped) completed");
    }

    @RunRequestScoped
    public void shutdownSharingPolicyDistribution() {
        stopDistribution();
        this.log.debug("shutdown() SharingPolicyDistribution (ApplicationScoped) completed");
    }

    private void saveForOwner(IndexDocument doc) {

    }

    private void saveForSharingPartners(IndexDocument doc) {

    }

    private void startDistribution() {

        this.exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                distribute();
            }
        }, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING, java.util.concurrent.TimeUnit.SECONDS);

    }

    /**
     * Fetches all elements from the queue and distributes them
     */
    private void distribute() {
        while (this.queue.size() > 0) {
            this.log.debug("Found" + this.queue.size() + " IndexDocument(s) in the queue to distribute");
            IndexDocument doc = this.queue.getNext();
            //TODO do something with it -> distribute
            System.out.println("Now distributing document");
        }
        //this.log.debug("Did not find an IndexDocument in the queue to distribute");
    }

    public void stopDistribution() {
        this.log.debug("SharingPolicyIndexDistribution stopping distribution thread");
        this.exec.shutdownNow();
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
