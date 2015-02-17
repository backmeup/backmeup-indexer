package org.backmeup.index.sharing.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.dal.QueuedIndexDocumentDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.serializer.Json;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

public class IndexDocumentDropOffTest {

    private IndexDocumentDropOffQueue queue;
    private QueuedIndexDocumentDao queuedIndexDocsDao;

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    @Before
    public void before() {
        this.queue = new IndexDocumentDropOffQueue();
        setupWhiteboxTest();
    }

    private void setupWhiteboxTest() {
        this.queuedIndexDocsDao = this.database.queuedIndexDocsDao;
        Whitebox.setInternalState(this.queue, "dao", this.queuedIndexDocsDao);
    }

    @Test
    public void testPollEmptyList() {
        IndexDocument doc = this.queue.getNext();
        assertNull(doc);
    }

    @Test
    public void testDropOffDocumentQueueFirstInFirstOut() {
        persistInTransaction(createIndexDocument(1L));
        assertEquals(1, this.queue.size());
        persistInTransaction(createIndexDocument(2L));
        persistInTransaction(createIndexDocument(3L));
        assertEquals(3, this.queue.size());
        IndexDocument doc = getNextInTransaction();
        assertEquals(1L, doc.getFields().get(IndexFields.FIELD_OWNER_ID));
        assertEquals(2, this.queue.size());
        assertEquals(2, this.queuedIndexDocsDao.getAllQueuedIndexDocuments().size());
    }

    private IndexDocument getNextInTransaction() {
        this.database.entityManager.getTransaction().begin();
        IndexDocument doc = this.queue.getNext();
        this.database.entityManager.getTransaction().commit();
        return doc;
    }

    private void persistInTransaction(IndexDocument indexDoc) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.queue.addIndexDocument(indexDoc);
        this.database.entityManager.getTransaction().commit();
    }

    @Test
    public void testSyncQueueFromDB() {
        //DB Testdata setup
        this.database.entityManager.getTransaction().begin();
        this.queuedIndexDocsDao.save(new QueuedIndexDocument(createIndexDocument(5L)));
        this.queuedIndexDocsDao.save(new QueuedIndexDocument(createIndexDocument(6L)));
        this.database.entityManager.getTransaction().commit();
        //now call sync
        this.queue.syncQueueAfterStartupFromDBRecords4JUnitTests();
        assertEquals(2, this.queue.size());
        IndexDocument doc = getNextInTransaction();
        assertEquals(5L, doc.getFields().get(IndexFields.FIELD_OWNER_ID));
        assertEquals(1, this.queue.size());
    }

    private IndexDocument createIndexDocument(Long userID) {
        try {
            File fIndexDocument = new File("src/test/resources/sampleIndexDocument.serindexdocument");
            String sampleFragment = FileUtils.readFileToString(fIndexDocument, "UTF-8");
            IndexDocument doc = Json.deserialize(sampleFragment, IndexDocument.class);
            doc.field(IndexFields.FIELD_OWNER_ID, userID);
            return doc;
        } catch (IOException e) {
            return new IndexDocument();
        }
    }
}
