package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes care of distributing newly created data coming from the queue, handed over by the indexing plugin
 * 
 * IndexDocument distribution. Fetches the IndexDocuments from the queue as they are handed over by the indexing plugin
 * and takes care of their distribution into the user's drop-off-for-inport zones according to the defined
 * SharingPolicies and triggers their to_import/to_delete process
 * 
 */
@ApplicationScoped
public class SharingPolicyImportNewPluginDataTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexDocumentDropOffQueue queue;
    @Inject
    private SharingPolicyExecution policyExecution;
    @Inject
    private SharingPolicyManager manager;

    @Override
    @RunRequestScoped
    public void run() {
        fetchDataFromQueue();
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
                distributeAndImportToOwner(doc);
                //distribute to all sharing users according to the policy set
                distributeAndImportToSharingPartners(doc);
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
    private void distributeAndImportToOwner(IndexDocument doc) throws IOException {
        this.policyExecution.executeImportOwner(doc);
    }

    /**
     * Takes an IndexDocument, analyzes its fields, distributes them to storage according to sharing policy, adds
     * toImport status records to DB
     * 
     * @param doc
     */
    private void distributeAndImportToSharingPartners(IndexDocument doc) throws IOException {
        //user that submitted this document within a themis workflow
        long ownerID = Long.parseLong(doc.getFields().get(IndexFields.FIELD_OWNER_ID).toString());

        //iterate over all active sharing policies that a given user has defined
        //Note: heritage sharings and other non-active policies will only be distributed once the owner logs into the system
        List<SharingPolicy> policies = this.manager.getAllActivePoliciesOwnedByUser(new User(ownerID));
        for (SharingPolicy policy : policies) {

            //add additional entries for sharing within the IndexDocument
            doc.field(IndexFields.FIELD_SHARED_BY_USER_ID, ownerID);
            //active user is always the document owner - reset the flag
            doc.field(IndexFields.FIELD_OWNER_ID, policy.getWithUserID());

            //check the different sharing policies and create according import tasks for doc
            this.policyExecution.executeImportSharingParnter(policy, doc);
        }
    }

}
