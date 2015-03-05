package org.backmeup.index.sharing.execution;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.SystemUtils;
import org.backmeup.index.ActiveUsers;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.dal.QueuedIndexDocumentDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.IndexDocumentTestingUtils;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy2DocumentUUIDConverter;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
import org.backmeup.index.storage.ThemisDataSink;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

/**
 * Test advanced IndexDocument distribution according to policies. Policy created first, policy created later -> import
 * all new and history (i.e. all index documents that have been created before) Policy deletion -> removal of
 * IndexDocuments
 *
 */
public class SharingPolicyIndexDocumentHistoryDistributorTest extends IndexDocumentTestingUtils {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();
    private QueuedIndexDocumentDao queuedIndexDocsDao;
    private IndexDocumentDropOffQueue queue;
    private SharingPolicyImportNewPluginDataTask distributeNewTask;
    private SharingPolicyUpToDateCheckerTask distributeExistingTask;
    private SharingPolicyExecution policyExecution;
    private SharingPolicyManager policyManager = SharingPolicyManager.getInstance();
    private SharingPolicy2DocumentUUIDConverter pol2uuidConverter;
    private ActiveUsers activeUsers;

    //fixed test set on sharing policies
    private SharingPolicy pol1w2;
    private User user1, user2;
    private Long currentTime;

    @Before
    public void before() {
        this.currentTime = new Date().getTime();

        this.distributeNewTask = new SharingPolicyImportNewPluginDataTask();
        this.distributeNewTask.setFrequency(1);
        this.distributeExistingTask = new SharingPolicyUpToDateCheckerTask();
        this.distributeExistingTask.setFrequency(1);
        setupWhiteboxTest();
    }

    @After
    public void after() {
        this.distributeNewTask.shutdownSharingPolicyExecution();
        this.distributeExistingTask.shutdownSharingPolicyExecution();
        cleanupTestData();
        this.policyManager.removeAllSharingPolicies();
    }

    @Test
    @Ignore("to complex to setup testing environment for this integration test")
    public void testShareAllPolicyAndImportExistingHistory() throws IOException, InterruptedException {
        Assume.assumeTrue(SystemUtils.IS_OS_WINDOWS);
        //setup the test scenario
        //a. create queue input data
        this.user1 = new User(1L);
        this.user2 = new User(2L);
        int jobID = 51;
        IndexDocument doc = createIndexDocument(this.user1.id());
        doc.field(IndexFields.FIELD_JOB_ID, jobID + "");
        doc.field(IndexFields.FIELD_BACKUP_AT, this.currentTime);
        //add document to import queue
        persistInTransaction(createQueuedIndexDocument(doc));
        //tell the queue to load existing records from the database
        this.queue.startupDroOffQueue();
        assertTrue(this.queue.size() > 0);
        assertTrue(this.queue.size() == this.queuedIndexDocsDao.getAllQueuedIndexDocuments().size());

        //start the distribution thread
        activateDistributionAndSleepJUnitThread4TwoSecs();
        //check if a serialized document ends up in the user's drop off space
        User owner1 = this.user1;
        User sharingp1 = this.user2;
        List<UUID> lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(owner1,
                ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertTrue("document distribution did not properly work", lUUIDs.size() == 1);
        assertTrue("document UUIDs don't match",
                lUUIDs.get(0).toString().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString()));

        //b. now create the sharing policy
        this.pol1w2 = this.policyManager.createSharingRule(this.user1, this.user2,
                SharingPolicies.SHARE_ALL_INKLUDING_OLD);

        //c. expecting data to show up into shared user's drop off space
        //give import a chance to kick-off
        Thread.sleep(2000);
        //check fragment still existing for owner
        lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(owner1, ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);
        assertTrue("document distributed to often", lUUIDs.size() == 1);
        assertTrue("document UUIDs don't match",
                lUUIDs.get(0).toString().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString()));

        //Info: For these test we overwrite ActiveUsers to return C for ThemisEncryptedPartition
        //check fragment existing for sharing partner
        lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(sharingp1,
                ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
        System.out.println(lUUIDs.size());
        assertTrue("document distribution did not properly work", lUUIDs.size() == 1);
        assertTrue("document UUIDs don't match",
                lUUIDs.get(0).toString().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString()));
    }

    private void activateDistributionAndSleepJUnitThread4TwoSecs() {
        //start the distribution thread
        try {
            this.database.entityManager.getTransaction().begin();
            this.distributeExistingTask.startupSharingPolicyExecution();
            this.distributeNewTask.startupSharingPolicyExecution();
            Thread.sleep(4000);
            this.database.entityManager.getTransaction().commit();
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    private void setupWhiteboxTest() {

        this.queuedIndexDocsDao = this.database.queuedIndexDocsDao;
        this.queue = new IndexDocumentDropOffQueue();
        Whitebox.setInternalState(this.queue, "dao", this.queuedIndexDocsDao);
        Whitebox.setInternalState(this.distributeNewTask, "queue", this.queue);

        this.policyExecution = new SharingPolicyExecution();
        Whitebox.setInternalState(this.policyExecution, "entryStatusDao", this.database.statusDao);
        Whitebox.setInternalState(this.distributeNewTask, "policyExecution", this.policyExecution);

        this.pol2uuidConverter = new SharingPolicy2DocumentUUIDConverter();
        this.activeUsers = new ActiveUsers() {
            @Override
            public String getMountedDrive(User user) {
                return "C";
            }

            @Override
            public List<User> getActiveUsers() {
                List<User> ret = new ArrayList<User>();
                ret.add(SharingPolicyIndexDocumentHistoryDistributorTest.this.user1);
                return ret;
            }

            @Override
            public boolean isUserActive(User user) {
                if (SharingPolicyIndexDocumentHistoryDistributorTest.this.user1.id() == user.id()) {
                    return true;
                }
                return false;
            }
        };
        Whitebox.setInternalState(this.activeUsers, "dao", this.database.indexManagerDao);
        Whitebox.setInternalState(this.distributeExistingTask, "activeUsers", this.activeUsers);
        Whitebox.setInternalState(this.distributeExistingTask, "pol2uuidConverter", this.pol2uuidConverter);
        Whitebox.setInternalState(this.distributeExistingTask, "policyExecution", this.policyExecution);
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
        QueuedIndexDocument qDoc = this.queuedIndexDocsDao.save(indexDoc);
        System.out.println("Queue ID: " + qDoc.getId());
        this.database.entityManager.getTransaction().commit();
    }

}
