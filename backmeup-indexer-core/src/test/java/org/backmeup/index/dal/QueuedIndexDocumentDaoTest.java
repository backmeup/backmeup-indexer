package org.backmeup.index.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.sharing.IndexDocumentTestingUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the JPA Hibernate storage and retrieval layer for queued index document via derby DB with
 * hibernate.hbm2ddl.auto=create
 */
public class QueuedIndexDocumentDaoTest extends IndexDocumentTestingUtils {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    private QueuedIndexDocumentDao queuedIndexDocsDao;

    @Before
    public void getDaoFromDb() {
        this.queuedIndexDocsDao = this.database.queuedIndexDocsDao;
    }

    @Test
    public void shouldStoreDocumentAndReadAllFromDB() {
        QueuedIndexDocument queuedDoc = createQueuedIndexDocument(1L);
        persistInTransaction(queuedDoc);

        List<QueuedIndexDocument> found = this.queuedIndexDocsDao.getAllQueuedIndexDocuments();
        assertNotNull(found);
        assertTrue(found.size() == 1);
        assertEquals(queuedDoc.getIndexDocument().getFields().size(), found.get(0).getIndexDocument().getFields()
                .size());
        assertEquals(queuedDoc.getIndexDocument().getFields().get("owner_name"), found.get(0).getIndexDocument()
                .getFields().get("owner_name"));
    }

    private void persistInTransaction(QueuedIndexDocument indexDoc) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.queuedIndexDocsDao.save(indexDoc);
        this.database.entityManager.getTransaction().commit();
    }

}
