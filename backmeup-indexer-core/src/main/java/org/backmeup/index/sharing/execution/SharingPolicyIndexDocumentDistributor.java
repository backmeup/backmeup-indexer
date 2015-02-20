package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
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
    @Inject
    private SharingPolicyManager manager = SharingPolicyManager.getInstance(); //TODO need to add bean and init methods in lifecycle

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

    private void startDistribution() {

        this.exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                distribute();
            }
        }, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stopDistribution() {
        this.log.debug("SharingPolicyIndexDistribution stopping distribution thread");
        this.exec.shutdownNow();
    }

    /**
     * Fetches all elements from the queue and distributes them
     */
    private void distribute() {
        while (this.queue.size() > 0) {
            this.log.debug("Found" + this.queue.size() + " IndexDocument(s) in the queue to distribute");
            System.out.println("Found" + this.queue.size() + " IndexDocument(s) in the queue to distribute");
            IndexDocument doc = this.queue.getNext();

            try {//distribute to owner
                distributeToOwner(doc);
                //distribute to all sharing users according to the policy set
                distributeToSharingPartners(doc);
            } catch (IOException e) {
                //TODO cleanup if one of the two operations failed?
                this.log.info("Exception distributing IndexDocument to user dropoffzone ", e);
                System.out.println(e.toString());
            }
        }
        //this.log.debug("Did not find an IndexDocument in the queue to distribute");
    }

    /**
     * Takes an IndexDocument, adds a documentID, and distributes it to the owner
     * 
     * @param doc
     */
    private void distributeToOwner(IndexDocument doc) throws IOException {
        long ownerID = Long.parseLong(doc.getFields().get(IndexFields.FIELD_OWNER_ID).toString());
        ThemisDataSink.saveIndexFragment(doc, new User(ownerID), ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);
    }

    /**
     * Takes an IndexDocument, analyzes its fields and distributes them according to policy
     * 
     * @param doc
     */
    private void distributeToSharingPartners(IndexDocument doc) throws IOException {
        //user that submitted this document within a themis workflow
        long ownerID = Long.parseLong(doc.getFields().get(IndexFields.FIELD_OWNER_ID).toString());

        //iterate over all sharing policies that a given user has defined
        List<SharingPolicy> policies = this.manager.getAllPoliciesForUser(new User(ownerID));
        for (SharingPolicy policy : policies) {

            doc.field(IndexFields.FIELD_SHARED_BY_USER_ID, ownerID);
            //active user is always the document owner - reset the flag
            doc.field(IndexFields.FIELD_OWNER_ID, policy.getWithUserID());

            //1. check sharing all with this user
            if (policy.getPolicy().equals(SharingPolicies.SHARE_ALL)) {
                ThemisDataSink.saveIndexFragment(doc, new User(policy.getWithUserID()),
                        ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);

            }
            //2. check if we're sharing this backup
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_BACKUP)) {
                //check if we're sharing this specific backupjob
                if ((policy.getSharedElementID() != null)
                        && (policy.getSharedElementID().equals(doc.getFields().get(IndexFields.FIELD_JOB_ID)))) {
                    ThemisDataSink.saveIndexFragment(doc, new User(policy.getWithUserID()),
                            ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
                }
            }
            //3. check if we're sharing this specific element/file
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_INDEX_DOCUMENT)) {
                //check if we're sharing this specific element
                if ((policy.getSharedElementID() != null)
                        && (policy.getSharedElementID().equals(doc.getFields().get(
                                IndexFields.FIELD_INDEX_DOCUMENT_UUID)))) {
                    ThemisDataSink.saveIndexFragment(doc, new User(policy.getWithUserID()),
                            ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
                }
            }
        }

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
