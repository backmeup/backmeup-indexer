package org.backmeup.index.sharing;

import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.sharing.execution.IndexDocumentDropOffQueue;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
import org.junit.Before;

public class IndexDocumentSharingIntegrationTest extends IndexDocumentTestingUtils {

    private SharingPolicyManager policyManager;
    private IndexDocumentDropOffQueue queue;

    //TODO get Derby into this. Persist Sharing Policies, test distribution;

    @Before
    public void before() {
        this.policyManager = SharingPolicyManager.getInstance();
        //this.queue = IndexDocumentDropOffQueue.getInstance();

    }

    public void testDistributionAccordingToSharingPolicyTest() {
        //create policy user 99991L shares all content with user 888881L
        SharingPolicy p = this.policyManager.createSharingRule(99991L, 888881L, SharingPolicies.SHARE_ALL);

        IndexDocument doc1 = createIndexDocument(99991L);
        this.queue.addIndexDocument(doc1);

    }

}
