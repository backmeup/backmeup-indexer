package org.backmeup.index.dal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.model.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the JPA Hibernate storage and retrieval layer for the IndexFragmentEntry Status DAO via derby DB with
 * hibernate.hbm2ddl.auto=create
 */
public class IndexFragmentEntryStatusDaoTest {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    private IndexFragmentEntryStatusDao statusDao;

    private User user1, user2;
    private UUID uuid1, uuid2, uuid3;
    private IndexFragmentEntryStatus status1, status2, status3, status4;

    @Before
    public void before() {
        this.statusDao = this.database.statusDao;
        createTestData();
    }

    @Test
    public void shouldStoreDocumentAndReadAllFromDBByUser() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllIndexFragmentEntryStatus(this.user1);
        assertTrue(found.size() == 2);
        //assert on sort order, first in first out
        assertTrue(found.get(0).getId() < found.get(1).getId());

        found = this.statusDao.getAllIndexFragmentEntryStatus(this.user1, StatusType.IMPORTED);
        assertNotNull(found);
        assertTrue(found.size() == 0);
        found = this.statusDao.getAllIndexFragmentEntryStatus(this.user1, StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 1);

        IndexFragmentEntryStatus status = this.statusDao.getIndexFragmentEntryStatus(this.user1, this.uuid1);
        assertNotNull(status);
        status = this.statusDao.getIndexFragmentEntryStatus(this.user1, this.uuid3);
        assertNull(status);
    }

    @Test
    public void shouldStoreDocumentAndReadAllFromDBByStatus() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllIndexFragmentEntryStatus(StatusType.DELETED);
        assertTrue(found.size() == 1);
        found = this.statusDao.getAllIndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 1);
        found = this.statusDao.getAllIndexFragmentEntryStatus(StatusType.IMPORTED);
        assertNotNull(found);
        assertTrue(found.size() == 0);
    }

    @Test
    public void shouldStoreDocumentAndReadAllFromDBByDocumentUUID() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllIndexFragmentEntryStatus(this.uuid2);
        assertTrue(found.size() == 2);
        assertTrue(found.get(0).getId() < found.get(1).getId());
    }

    @Test
    public void shouldStoreDocumentAndQueryByEntityID() {
        persistTestData();
        IndexFragmentEntryStatus found = this.statusDao.findById(this.status1.getId());
        assertNotNull(found);
        assertTrue(found.getDocumentUUID().equals(this.uuid1));
        found = this.statusDao.findById(66L);
        assertNull(found);
    }

    private void createTestData() {
        this.user1 = new User(1L);
        this.user2 = new User(2L);

        this.uuid1 = UUID.randomUUID();
        this.uuid2 = UUID.randomUUID();
        this.uuid3 = UUID.randomUUID();

        this.status1 = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT, this.uuid1, true, this.user1);

        this.status2 = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_DELETION, this.uuid2, true, this.user1);

        this.status3 = new IndexFragmentEntryStatus(StatusType.DELETED, this.uuid3, true, this.user2);

        this.status4 = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_DELETION, this.uuid2, true, this.user2);
    }

    private void persistTestData() {
        this.status1 = persistInTransaction(this.status1);
        this.status2 = persistInTransaction(this.status2);
        this.status3 = persistInTransaction(this.status3);
        this.status4 = persistInTransaction(this.status4);
    }

    private IndexFragmentEntryStatus persistInTransaction(IndexFragmentEntryStatus entryStatus) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        entryStatus = this.statusDao.save(entryStatus);
        this.database.entityManager.getTransaction().commit();
        return entryStatus;
    }

}
