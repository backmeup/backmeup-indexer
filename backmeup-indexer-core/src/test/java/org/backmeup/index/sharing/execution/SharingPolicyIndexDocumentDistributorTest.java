package org.backmeup.index.sharing.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.dal.QueuedIndexDocumentDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.IndexDocumentTestingUtils;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
import org.backmeup.index.storage.ThemisDataSink;
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
    private SharingPolicyImportNewPluginDataTask policyExecutionTask;
    private SharingPolicyImportNewPluginDataTaskLauncher policyExecutionTaskLauncher;
    private SharingPolicyExecution policyExecution;
    private SharingPolicyManager policyManager;

    //fixed test set on sharing policies
    private SharingPolicy pol1w2, pol1w7, pol3w4, pol5w6, pol1w8, pol1w9;
    private User user1, user2, user3, user4, user5, user6, user7, user8, user9;
    private Date dateAfterBackup, dateBeforeBackup;

    Long currentTime = new Date().getTime();

    @Before
    public void before() {
        setupWhiteboxTest();
    }

    @After
    public void after() {
        this.policyExecutionTaskLauncher.shutdownSharingPolicyExecution();
        cleanupTestData();
    }

    @Test
    public void testFetchingElementsFromQueue() throws InterruptedException {
        //start the distribution thread
        activateQueueAndSleepJUnitThread4TwoSecs();
        //check the queue has been processed
        assertTrue("Expected queue size of 0 but actually was: " + this.queue.size(), this.queue.size() == 0);
        assertTrue("Expected queue size of 0 but actually was: "
                + this.queuedIndexDocsDao.getAllQueuedIndexDocuments().size(), this.queuedIndexDocsDao
                .getAllQueuedIndexDocuments().size() == 0);
    }

    @Test
    public void testLoadTestDataInQueue() {
        assertTrue(this.queue.size() > 0);
        assertTrue(this.queue.size() == this.queuedIndexDocsDao.getAllQueuedIndexDocuments().size());
    }

    @Test
    public void testDistributionOfSerializedIndexDocsToDropOffUserSpaceWithSharingRuleShareAllNewer()
            throws IOException {
        //start the distribution thread
        activateQueueAndSleepJUnitThread4TwoSecs();
        //check if a serialized document ends up in the user's drop off space
        User owner1 = this.user1;
        User sharingp1 = this.user8;
        List<UUID> lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(sharingp1,
                ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
        //case1a: where no sharing policy exists between users
        assertTrue(lUUIDs.size() == 0);

        sharingp1 = this.user9;
        lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(sharingp1,
                ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
        //case1b: where sharing policy existed at document drop off time
        assertTrue(lUUIDs.size() == 1);
        UUID uuid = lUUIDs.get(0);
        IndexDocument docSharingp1 = ThemisDataSink.getIndexFragment(uuid, sharingp1,
                ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
        assertNotNull(docSharingp1);
        assertEquals(sharingp1.id() + "", docSharingp1.getFields().get(IndexFields.FIELD_OWNER_ID).toString());
        assertEquals(uuid.toString(), docSharingp1.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());
        assertEquals(owner1.id().toString(), docSharingp1.getFields().get(IndexFields.FIELD_SHARED_BY_USER_ID)
                .toString());

        //check that the proper waiting for import status types were created
        List<IndexFragmentEntryStatus> lDBStatus = this.database.statusDao
                .getAllFromUserInOneOfTheTypesAndByUserAsDocumentOwner(owner1, StatusType.WAITING_FOR_IMPORT);
        assertNotNull(lDBStatus);
        assertTrue(lDBStatus.size() == 1);
        assertEquals("missing proper import status for owner", StatusType.WAITING_FOR_IMPORT, lDBStatus.get(0)
                .getStatusType());
        lDBStatus = this.database.statusDao.getAllFromUserInOneOfTheTypesAndByDocumentOwner(sharingp1, owner1,
                StatusType.WAITING_FOR_IMPORT);
        assertNotNull(lDBStatus);
        assertTrue(lDBStatus.size() == 1);
        assertEquals("missing proper import status for sharingpartner", StatusType.WAITING_FOR_IMPORT, lDBStatus.get(0)
                .getStatusType());
    }

    @Test
    public void testDistributionOfSerializedIndexDocsToDropOffUserSpaceWithSharingRules() {
        //start the distribution thread
        activateQueueAndSleepJUnitThread4TwoSecs();
        //check if a serialized document ends up in the user's drop off space
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
            this.policyExecutionTaskLauncher.startupSharingPolicyExecution();
            Thread.sleep(2000);
            this.database.entityManager.getTransaction().commit();
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    private void setupWhiteboxTest() {
        this.policyManager = new SharingPolicyManager();
        Whitebox.setInternalState(this.policyManager, "sharingPolicyDao", this.database.sharingPolicyDao);

        this.queuedIndexDocsDao = this.database.queuedIndexDocsDao;
        this.queue = new IndexDocumentDropOffQueue();
        this.policyExecution = new SharingPolicyExecution();
        Whitebox.setInternalState(this.queue, "dao", this.queuedIndexDocsDao);
        Whitebox.setInternalState(this.policyExecution, "entryStatusDao", this.database.statusDao);

        this.policyExecutionTask = new SharingPolicyImportNewPluginDataTask();
        this.policyExecutionTaskLauncher = new SharingPolicyImportNewPluginDataTaskLauncher();
        this.policyExecutionTaskLauncher.setFrequency(1);

        Whitebox.setInternalState(this.policyExecutionTask, "queue", this.queue);
        Whitebox.setInternalState(this.policyExecutionTask, "policyExecution", this.policyExecution);
        Whitebox.setInternalState(this.policyExecutionTask, "manager", this.policyManager);
        Whitebox.setInternalState(this.policyExecutionTaskLauncher, "task", this.policyExecutionTask);

        createQueueInputData();
        createSharingPolicyData();
        this.queue.startupDroOffQueue();
    }

    private void createSharingPolicyData() {
        //policy 1a -> share all data inkluding the ones before the policy was created
        this.user1 = new User(1L);
        this.user2 = new User(2L);
        this.user7 = new User(7L);
        this.database.entityManager.getTransaction().begin();
        this.pol1w2 = this.policyManager.createAndAddSharingPolicy(this.user1, this.user2,
                SharingPolicies.SHARE_ALL_INKLUDING_OLD);
        this.pol1w7 = this.policyManager.createAndAddSharingPolicy(this.user1, this.user7,
                SharingPolicies.SHARE_ALL_INKLUDING_OLD);
        this.database.entityManager.getTransaction().commit();

        //policy 1b -> share all data but only data that has been created after the policy
        this.user8 = new User(8L);
        this.user9 = new User(9L);
        this.pol1w8 = new SharingPolicy(this.user1, this.user8, SharingPolicies.SHARE_ALL_AFTER_NOW, "My Name",
                "My Description");

        this.pol1w9 = new SharingPolicy(this.user1, this.user9, SharingPolicies.SHARE_ALL_AFTER_NOW, "My Name",
                "My Description");
        int hours = 2; //create a date in history 
        this.dateAfterBackup = new Date(this.currentTime + hours * 60 * 60 * 1000);
        this.dateBeforeBackup = new Date(this.currentTime - hours * 60 * 60 * 1000);
        this.pol1w9.setPolicyCreationDate(this.dateBeforeBackup);
        this.pol1w8.setPolicyCreationDate(this.dateAfterBackup);
        this.database.entityManager.getTransaction().begin();
        this.policyManager.addSharingPolicy(this.pol1w8);
        this.policyManager.addSharingPolicy(this.pol1w9);
        this.database.entityManager.getTransaction().commit();

        //policy 2 -> share all elements of a specific backup job 53
        this.user3 = new User(3L);
        this.user4 = new User(4L);
        this.database.entityManager.getTransaction().begin();
        this.pol3w4 = this.policyManager.createAndAddSharingPolicy(this.user3, this.user4,
                SharingPolicies.SHARE_BACKUP, "53", "My Name", "My Description");
        this.database.entityManager.getTransaction().commit();

        //policy 3 -> share a specific index-document, which however is not reflected in our testdata
        this.user5 = new User(5L);
        this.user6 = new User(6L);
        this.database.entityManager.getTransaction().begin();
        this.pol5w6 = new SharingPolicy(this.user5, this.user6, SharingPolicies.SHARE_INDEX_DOCUMENT, "My Name",
                "My Description");
        this.pol5w6.setSharedElementID(UUID.randomUUID().toString());
        this.policyManager.addSharingPolicy(this.pol5w6);
        this.database.entityManager.getTransaction().commit();

        //by default is set to wait for sharing partner handshake - trigger policy activated
        setPolicyHasBeenApprovedBySharingPartner(this.pol1w2);
        setPolicyHasBeenApprovedBySharingPartner(this.pol1w7);
        setPolicyHasBeenApprovedBySharingPartner(this.pol5w6);
        setPolicyHasBeenApprovedBySharingPartner(this.pol1w9);
        setPolicyHasBeenApprovedBySharingPartner(this.pol1w8);
        setPolicyHasBeenApprovedBySharingPartner(this.pol3w4);
    }

    private void setPolicyHasBeenApprovedBySharingPartner(SharingPolicy p) {
        this.database.entityManager.getTransaction().begin();
        p.setState(ActivityState.ACCEPTED_AND_ACTIVE);
        this.database.sharingPolicyDao.merge(p);
        this.database.entityManager.getTransaction().commit();
    }

    private void createQueueInputData() {
        for (int i = 1; i <= 15; i++) {
            IndexDocument doc = createIndexDocument(new Long(i));
            doc.field(IndexFields.FIELD_JOB_ID, i + 50 + "");
            doc.field(IndexFields.FIELD_BACKUP_AT, this.currentTime);
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
