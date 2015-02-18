package org.backmeup.index.sharing.execution;

import static org.junit.Assert.assertTrue;

import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.dal.QueuedIndexDocumentDao;
import org.backmeup.index.sharing.IndexDocumentTestingUtils;
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

    @Before
    public void before() {
        this.distributor = new SharingPolicyIndexDocumentDistributor();
        this.distributor.setFrequency(1);
        setupWhiteboxTest();
    }

    @After
    public void after() {
        this.distributor.shutdownSharingPolicyDistribution();
    }

    @Test
    public void testDistributionOfIndexDocuments() throws InterruptedException {
        //start the distribution thread
        this.database.entityManager.getTransaction().begin();
        this.distributor.startupSharingPolicyDistribution();
        Thread.sleep(2000);
        this.database.entityManager.getTransaction().commit();
        //check the queue has been processed
        assertTrue(this.queue.size() == 0);
        assertTrue(this.queuedIndexDocsDao.getAllQueuedIndexDocuments().size() == 0);
    }

    @Test
    public void testLoadTestDataInQueue() {
        assertTrue(this.queue.size() > 0);
        assertTrue(this.queue.size() == this.queuedIndexDocsDao.getAllQueuedIndexDocuments().size());
    }

    private void setupWhiteboxTest() {
        this.queuedIndexDocsDao = this.database.queuedIndexDocsDao;
        this.queue = new IndexDocumentDropOffQueue();
        Whitebox.setInternalState(this.queue, "dao", this.queuedIndexDocsDao);
        Whitebox.setInternalState(this.distributor, "queue", this.queue);
        createQueueInputData();
        this.queue.syncQueueAfterStartupFromDBRecords4JUnitTests();
    }

    private void createQueueInputData() {
        for (int i = 0; i < 15; i++) {
            persistInTransaction(createConfig(new Long(i)));
        }
    }

    private void persistInTransaction(QueuedIndexDocument indexDoc) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.queuedIndexDocsDao.save(indexDoc);
        this.database.entityManager.getTransaction().commit();
    }

}
