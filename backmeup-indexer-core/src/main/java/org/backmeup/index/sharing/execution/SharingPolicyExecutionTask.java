package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
import org.backmeup.index.storage.ThemisDataSink;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of
 * 
 * a) IndexDocument distribution. Fetches the IndexDocuments from the queue as they are handed over by the indexing
 * plugin and takes care of their distribution into the user's drop-off-for-inport zones according to the defined
 * SharingPolicies
 * 
 * b) Enforcing SharingPolicies Checks if policies have changed and updates status entries to_import / to_delete
 *
 */
@ApplicationScoped
public class SharingPolicyExecutionTask {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    private int SECONDS_BETWEEN_RECHECKING = 2;

    @Inject
    private IndexDocumentDropOffQueue queue;
    @Inject
    private SharingPolicyExecution policyExecution;
    @Inject
    private SharingPolicyManager manager = SharingPolicyManager.getInstance(); //TODO need to add bean and init methods in lifecycle

    @RunRequestScoped
    public void startupSharingPolicyExecution() {
        startPolicyExecution();
        this.log.debug("startup() SharingPolicyExecution (ApplicationScoped) completed");
    }

    @RunRequestScoped
    public void shutdownSharingPolicyExecution() {
        stopPolicyExecution();
        this.log.debug("shutdown() SharingPolicyDistribution (ApplicationScoped) completed");
    }

    private void startPolicyExecution() {

        this.exec.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                fetchDataFromQueue();
            }
        }, this.SECONDS_BETWEEN_RECHECKING, this.SECONDS_BETWEEN_RECHECKING, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stopPolicyExecution() {
        this.log.debug("SharingPolicyExecutionTask stopping distribution thread");
        this.exec.shutdownNow();
    }

    /**
     * Fetches all elements from the queue and distributes them to the user drop off space
     */
    private void fetchDataFromQueue() {
        while (this.queue.size() > 0) {
            this.log.debug("Found" + this.queue.size() + " IndexDocument(s) in the queue to distribute");
            //get next element from the drop off queue 
            IndexDocument doc = this.queue.getNext();

            try {//distribute to owner
                distributeToOwner(doc);
                //distribute to all sharing users according to the policy set
                distributeToSharingPartners(doc);
            } catch (IOException e) {
                //TODO cleanup if one of the two operations failed?
                this.log.info("Exception distributing IndexDocument to user dropoffzone ", e);
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
        //TODO AL need to check if we already know this document
        long ownerID = Long.parseLong(doc.getFields().get(IndexFields.FIELD_OWNER_ID).toString());
        String uuid = doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString();
        ThemisDataSink.saveIndexFragment(doc, new User(ownerID), ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);

        this.log.debug("distributed and stored IndexFragment: " + uuid + " for userID: " + ownerID
                + " TO_IMPORT_USER_OWNED drop-off;  added status: toImport to DB");
    }

    /**
     * Takes an IndexDocument, analyzes its fields, distributes them to storage according to sharing policy, adds
     * toImport status records to DB
     * 
     * @param doc
     */
    private void distributeToSharingPartners(IndexDocument doc) throws IOException {
        //user that submitted this document within a themis workflow
        long ownerID = Long.parseLong(doc.getFields().get(IndexFields.FIELD_OWNER_ID).toString());
        String uuid = doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString();

        //iterate over all sharing policies that a given user has defined
        List<SharingPolicy> policies = this.manager.getAllPoliciesForUser(new User(ownerID));
        for (SharingPolicy policy : policies) {

            //add additional entries for sharing within the IndexDocument
            doc.field(IndexFields.FIELD_SHARED_BY_USER_ID, ownerID);
            //active user is always the document owner - reset the flag
            doc.field(IndexFields.FIELD_OWNER_ID, policy.getWithUserID());

            //check the different sharing policies and create according import tasks for doc
            this.policyExecution.executeImport(policy, doc);
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