package org.backmeup.index.dal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.backmeup.index.core.model.IndexFragmentEntryStatus;
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
    private IndexFragmentEntryStatus status1, status2, status3;

    @Before
    public void getDaoFromDb() {
        this.statusDao = this.database.statusDao;
    }

    @Before
    public void before() {
        createTestData();
    }

    @Test
    public void shouldStoreDocumentAndReadAllFromDBForUser() {

        persistInTransaction(this.status1);
        persistInTransaction(this.status2);
        persistInTransaction(this.status3);
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllIndexFragmentEntryStatus(this.user1);
        assertNotNull(found);
        assertTrue(found.size() == 2);

        //TODO AL continue here and cover all testcases
    }

    private void createTestData() {
        this.user1 = new User(1L);
        this.user2 = new User(2L);

        this.uuid1 = UUID.randomUUID();
        this.uuid2 = UUID.randomUUID();
        this.uuid3 = UUID.randomUUID();

        this.status1 = new IndexFragmentEntryStatus(IndexFragmentEntryStatus.StatusType.WAITING_FOR_IMPORT,
                this.uuid1.toString(), this.user1.id());

        this.status2 = new IndexFragmentEntryStatus(IndexFragmentEntryStatus.StatusType.WAITING_FOR_IMPORT,
                this.uuid2.toString(), this.user1.id());

        this.status3 = new IndexFragmentEntryStatus(IndexFragmentEntryStatus.StatusType.DELETED, this.uuid3.toString(),
                this.user2.id());

    }

    private void persistInTransaction(IndexFragmentEntryStatus entryStatus) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.statusDao.save(entryStatus);
        this.database.entityManager.getTransaction().commit();
    }

}
