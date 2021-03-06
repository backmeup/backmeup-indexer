package org.backmeup.index.sharing;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.backmeup.index.IndexManagerIntegrationTestSetup;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.dal.QueuedIndexDocumentDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.execution.IndexContentUpdateTask;
import org.backmeup.index.sharing.execution.IndexContentUpdateTaskLauncher;
import org.backmeup.index.sharing.execution.IndexDocumentDropOffQueue;
import org.backmeup.index.sharing.execution.SharingPolicyImportNewPluginDataTask;
import org.backmeup.index.sharing.execution.SharingPolicyImportNewPluginDataTaskLauncher;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.storage.ThemisDataSink;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

public class IndexDocumentSharingIntegrationTest extends IndexManagerIntegrationTestSetup {

    private IndexDocumentDropOffQueue droppOffqueue;
    private QueuedIndexDocumentDao queuedIndexDocsDao;
    private SharingPolicyImportNewPluginDataTaskLauncher distributorLauncher;
    private SharingPolicyImportNewPluginDataTask distributor;
    private IndexContentUpdateTask checkImports;
    private IndexContentUpdateTaskLauncher checkImportsLauncher;

    private static final User userOwner = new User(999991L);
    private static final User userSharingP = new User(999992L);

    @Ignore("Setup to get this scenario working within a UnitTest is too complex")
    @Test
    public void testSharingContentBetweenUsers() throws InterruptedException {
        //create sharing policy between two users
        this.database.entityManager.getTransaction().begin();
        this.sharingPolicyManager.createAndAddSharingPolicy(userOwner, userSharingP,
                SharingPolicies.SHARE_ALL_AFTER_NOW);
        this.database.entityManager.getTransaction().commit();

        IndexDocument doc1 = IndexDocumentTestingUtils.createIndexDocument(userOwner.id());
        doc1.field(IndexFields.FIELD_INDEX_DOCUMENT_UUID, UUID.randomUUID().toString());
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.droppOffqueue.addIndexDocument(doc1);
        this.database.entityManager.getTransaction().commit();

        //give the document distribution a chance to kick-off, the checkImport tasks will trigger ES import 
        Thread.sleep(4000);
        List<IndexFragmentEntryStatus> status = this.contentStatusDao.getAllFromUserOfType(userOwner,
                StatusType.WAITING_FOR_IMPORT);
        status = this.contentStatusDao.getAllFromUserOfType(userSharingP, StatusType.WAITING_FOR_IMPORT);
        assertTrue(status.size() > 0);

        //TODO need to overwrite the getActive users to make ContentManager Import work
    }

    @Before
    public void beforeTest() {
        setupWhiteboxTest();
        //start the components
        this.droppOffqueue.startupDroOffQueue();
        this.checkImportsLauncher.setFrequency(1);
        this.checkImportsLauncher.startupIndexContentUpdateExecution();
    }

    @After
    public void afterTest() {
        this.checkImportsLauncher.shutdownIndexContentUpdateExecution();
        this.distributorLauncher.shutdownSharingPolicyExecution();
        cleanupTestData();
    }

    private void setupWhiteboxTest() {
        this.droppOffqueue = new IndexDocumentDropOffQueue();
        this.distributor = new SharingPolicyImportNewPluginDataTask();
        this.distributorLauncher = new SharingPolicyImportNewPluginDataTaskLauncher();
        this.checkImports = new IndexContentUpdateTask();
        this.checkImportsLauncher = new IndexContentUpdateTaskLauncher();
        this.queuedIndexDocsDao = this.database.queuedIndexDocsDao;

        Whitebox.setInternalState(this.droppOffqueue, "dao", this.queuedIndexDocsDao);
        Whitebox.setInternalState(this.distributor, "queue", this.droppOffqueue);
        Whitebox.setInternalState(this.distributorLauncher, "task", this.distributor);

        Whitebox.setInternalState(this.checkImportsLauncher, "task", this.checkImports);
    }

    private void cleanupTestData() {
        try {
            ThemisDataSink.deleteDataSinkHome(userOwner);
            ThemisDataSink.deleteDataSinkHome(userSharingP);
        } catch (IllegalArgumentException e) {
        }
    }

}
