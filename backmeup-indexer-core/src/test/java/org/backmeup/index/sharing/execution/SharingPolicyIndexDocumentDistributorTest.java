package org.backmeup.index.sharing.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.dal.QueuedIndexDocumentDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.IndexDocumentTestingUtils;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

public class SharingPolicyIndexDocumentDistributorTest extends IndexDocumentTestingUtils {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();
    private QueuedIndexDocumentDao queuedIndexDocsDao;
    private IndexDocumentDropOffQueue queue;
    private SharingPolicyIndexDocumentDistributor distributor;
    private SharingPolicyManager policyManager = SharingPolicyManager.getInstance();

    //fixed test set on sharing policies
    private SharingPolicy pol12, pol17, pol34, pol56;
    User user1, user2, user3, user4, user5, user6, user7;

    @Before
    public void before() {
        this.distributor = new SharingPolicyIndexDocumentDistributor();
        this.distributor.setFrequency(1);
        setupWhiteboxTest();
    }

    @After
    public void after() {
        this.distributor.shutdownSharingPolicyDistribution();
        cleanupTestData();
    }

    @Test
    public void testFetchingElementsFromQueue() throws InterruptedException {
        //start the distribution thread
        activateQueueAndSleepJUnitThread4TwoSecs();
        //check the queue has been processed
        assertTrue(this.queue.size() == 0);
        assertTrue(this.queuedIndexDocsDao.getAllQueuedIndexDocuments().size() == 0);
    }

    @Test
    public void testLoadTestDataInQueue() {
        assertTrue(this.queue.size() > 0);
        assertTrue(this.queue.size() == this.queuedIndexDocsDao.getAllQueuedIndexDocuments().size());
    }

    @Test
    public void testDistributionOfSerializedIndexDocsToDropOffUserSpaceWithSharingRules() {
        //start the distribution thread
        activateQueueAndSleepJUnitThread4TwoSecs();
        //check if a serealized document ends up in the user's drop off space
        User owner = this.user1;
        User sharingp1 = this.user2;
        User sharingp2 = this.user7;
        List<UUID> lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(owner,
                ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertTrue(lUUIDs.size() == 1);

        //check if the proper UUID is reflected and the index document fields have been written for owner and sharing partner
        UUID uuid = lUUIDs.get(0);
        IndexDocument docOwner, docSharingp1, docSharingp2;
        try {
            docOwner = ThemisDataSink.getIndexFragment(uuid, owner,
                    ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);
        } catch (IOException e) {
            docOwner = null;
        }
        assertNotNull(docOwner);
        assertEquals(owner.id() + "", docOwner.getFields().get(IndexFields.FIELD_OWNER_ID).toString());
        assertEquals(uuid.toString(), docOwner.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());

        //check if the sharing policies were properly distributed
        try {
            docSharingp1 = ThemisDataSink.getIndexFragment(uuid, sharingp1,
                    ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
            docSharingp2 = ThemisDataSink.getIndexFragment(uuid, sharingp2,
                    ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
        } catch (IOException e) {
            e.printStackTrace();
            docSharingp1 = null;
            docSharingp2 = null;
        }
        //check sharing rules correctly performed
        assertNotNull(docSharingp1);
        assertEquals(sharingp1.id() + "", docSharingp1.getFields().get(IndexFields.FIELD_OWNER_ID).toString());
        assertEquals(uuid.toString(), docSharingp1.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());
        assertEquals(owner.id().toString(), docSharingp1.getFields().get(IndexFields.FIELD_SHARED_BY_USER_ID)
                .toString());

        assertNotNull(docSharingp2);
        assertEquals(sharingp2.id() + "", docSharingp2.getFields().get(IndexFields.FIELD_OWNER_ID).toString());
        assertEquals(uuid.toString(), docSharingp2.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());
        assertEquals(owner.id().toString(), docSharingp2.getFields().get(IndexFields.FIELD_SHARED_BY_USER_ID)
                .toString());

    }

    private void activateQueueAndSleepJUnitThread4TwoSecs() {
        //start the distribution thread
        try {
            this.database.entityManager.getTransaction().begin();
            this.distributor.startupSharingPolicyDistribution();
            Thread.sleep(2000);
            this.database.entityManager.getTransaction().commit();
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    private void setupWhiteboxTest() {
        this.queuedIndexDocsDao = this.database.queuedIndexDocsDao;
        this.queue = new IndexDocumentDropOffQueue();
        Whitebox.setInternalState(this.queue, "dao", this.queuedIndexDocsDao);
        Whitebox.setInternalState(this.distributor, "queue", this.queue);
        createQueueInputData();
        createSharingPolicyData();
        this.queue.syncQueueAfterStartupFromDBRecords4JUnitTests();
    }

    private void createSharingPolicyData() {
        this.user1 = new User(1L);
        this.user2 = new User(2L);
        this.user7 = new User(7L);
        this.pol12 = this.policyManager.createSharingRule(this.user1, this.user2, SharingPolicies.SHARE_ALL);
        this.pol17 = this.policyManager.createSharingRule(this.user1, this.user7, SharingPolicies.SHARE_ALL);

        //policy 2 -> share all elements of a specific backup job 53
        this.user3 = new User(3L);
        this.user4 = new User(4L);
        this.pol34 = this.policyManager.createSharingRule(this.user3, this.user4, SharingPolicies.SHARE_BACKUP, "53");

        //policy 3 -> share a specific index-document, which however is not reflected in our testdata
        this.user5 = new User(5L);
        this.user6 = new User(6L);
        this.pol56 = new SharingPolicy(this.user5, this.user6, SharingPolicies.SHARE_INDEX_DOCUMENT);
        this.pol56.setSharedElementID(UUID.randomUUID().toString());

    }

    private void createQueueInputData() {
        for (int i = 1; i <= 15; i++) {
            IndexDocument doc = createIndexDocument(new Long(i));
            doc.field(IndexFields.FIELD_JOB_ID, i + 50 + "");
            persistInTransaction(createQueuedIndexDocument(doc));
        }
    }

    private void cleanupTestData() {
        for (int i = 1; i <= 15; i++) {
            try {
                ThemisDataSink.deleteAllIndexFragments(new User(new Long(i)),
                        ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);
            } catch (IOException e) {
            }
            try {
                ThemisDataSink.deleteAllIndexFragments(new User(new Long(i)),
                        ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
            } catch (IOException e) {
            }
            try {
                ThemisDataSink.deleteDataSinkHome(new User(new Long(i)));
            } catch (IllegalArgumentException e) {
            }
        }
    }

    private void persistInTransaction(QueuedIndexDocument indexDoc) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.queuedIndexDocsDao.save(indexDoc);
        this.database.entityManager.getTransaction().commit();
    }

}
