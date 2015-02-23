package org.backmeup.index;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.data.dummy.ThemisDataSink.IndexFragmentType;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.IndexDocumentTestingUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexContentManagerIntegrationTest extends IndexManagerIntegrationTestSetup {

    private User testUser1 = new User(999992L);

    @Before
    public void setupTest() throws IOException {
        createTestData();
    }

    @After
    public void shutdownTest() {
        cleanupTestData();
    }

    @Test
    public void testContentManagerUpdateOperation() throws IOException {
        this.contentManager.executeContentUpdates(this.testUser1);

        //check we've started an ES instance
        RunningIndexUserConfig conf = this.runningInstancesdao.findConfigByUser(this.testUser1);
        assertNotNull(conf);

        List<IndexFragmentEntryStatus> lStatus = this.contentStatusDao.getAllIndexFragmentEntryStatus(this.testUser1,
                StatusType.IMPORTED);
        assertTrue(lStatus.size() > 0);
    }

    private void createTestData() throws IOException {
        //create the TestDocument
        UUID documentUUID = UUID.randomUUID();
        IndexDocument doc = IndexDocumentTestingUtils.createIndexDocument(this.testUser1.id());
        doc.field(IndexFields.FIELD_INDEX_DOCUMENT_UUID, documentUUID.toString());

        //Add it to the Data Storage Layer
        ThemisDataSink.saveIndexFragment(doc, this.testUser1, IndexFragmentType.TO_IMPORT_USER_OWNED);

        //Create an entry for it within the Database
        IndexFragmentEntryStatus status1 = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT, documentUUID,
                true, this.testUser1.id());
        persistInTransaction(status1);
    }

    private IndexFragmentEntryStatus persistInTransaction(IndexFragmentEntryStatus entryStatus) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        entryStatus = this.contentStatusDao.save(entryStatus);
        this.database.entityManager.getTransaction().commit();
        return entryStatus;
    }

    private void cleanupTestData() {
        try {
            ThemisDataSink.deleteDataSinkHome(this.testUser1);
        } catch (IllegalArgumentException e) {
        }
    }

}
