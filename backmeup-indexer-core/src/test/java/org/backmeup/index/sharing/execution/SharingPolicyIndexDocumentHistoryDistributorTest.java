package org.backmeup.index.sharing.execution;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
import org.backmeup.index.storage.ThemisDataSink;
import org.junit.After;
import org.junit.Before;
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
    private SharingPolicyImportNewPluginDataTask policyExecutionTask;
    private SharingPolicyExecution policyExecution;
    private SharingPolicyManager policyManager = SharingPolicyManager.getInstance();

    //fixed test set on sharing policies
    private SharingPolicy pol1w2;
    private User user1, user2;
    private Long currentTime;

    @Before
    public void before() {
        this.currentTime = new Date().getTime();

        this.policyExecutionTask = new SharingPolicyImportNewPluginDataTask();
        this.policyExecutionTask.setFrequency(1);
        setupWhiteboxTest();
    }

    @After
    public void after() {
        this.policyExecutionTask.shutdownSharingPolicyExecution();
        cleanupTestData();
        this.policyManager.removeAllSharingPolicies();
    }

    @Test
    public void testShareAllPolicyAndImportExistingHistory() throws IOException, InterruptedException {
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

        //start the distribution thread
        activateQueueAndSleepJUnitThread4TwoSecs();
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
        //give inport a chance to kick-off
        Thread.sleep(2000);
        //check fragment still existing for owner
        lUUIDs = ThemisDataSink.getAllIndexFragmentUUIDs(sharingp1,
                ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
        assertFalse("document distributed to often", lUUIDs.size() > 1);
        assertTrue("document UUIDs don't match",
                lUUIDs.get(0).toString().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString()));
        //check fragment existing for sharing partner
        assertTrue("document distribution did not properly work", lUUIDs.size() == 1);
        assertTrue("document UUIDs don't match",
                lUUIDs.get(0).toString().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString()));
    }

    private void activateQueueAndSleepJUnitThread4TwoSecs() {
        //start the distribution thread
        try {
            this.database.entityManager.getTransaction().begin();
            this.policyExecutionTask.startupSharingPolicyExecution();
            Thread.sleep(2000);
            this.database.entityManager.getTransaction().commit();
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    private void setupWhiteboxTest() {
        this.queuedIndexDocsDao = this.database.queuedIndexDocsDao;
        this.queue = new IndexDocumentDropOffQueue();
        this.policyExecution = new SharingPolicyExecution();
        Whitebox.setInternalState(this.queue, "dao", this.queuedIndexDocsDao);
        Whitebox.setInternalState(this.policyExecutionTask, "queue", this.queue);
        Whitebox.setInternalState(this.policyExecution, "entryStatusDao", this.database.statusDao);
        Whitebox.setInternalState(this.policyExecutionTask, "policyExecution", this.policyExecution);
        this.queue.startupDroOffQueue();
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
