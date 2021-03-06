package org.backmeup.index.dal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
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

    private User user1, user2, user3, user4;
    private UUID uuid1, uuid2, uuid3, uuid4;
    private IndexFragmentEntryStatus status1, status2, status3, status4, status5;
    private Long backupJobID1, backupJobID2;
    private Date dateAfterBackup, dateBeforeBackup, dateNow;
    private Long currentTime = new Date().getTime();

    @Before
    public void before() {
        this.statusDao = this.database.statusDao;
        createTestData();
    }

    @Test
    public void getAllByUser() {
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllFromUser(this.user1);
        assertTrue(found.size() == 0);
        persistTestData();
        found = this.statusDao.getAllFromUser(this.user1);
        assertTrue(found.size() == 2);
        //assert on sort order, first in first out
        assertTrue(found.get(0).getId() < found.get(1).getId());

        found = this.statusDao.getAllFromUserOfType(this.user1, StatusType.IMPORTED);
        assertNotNull(found);
        assertTrue(found.size() == 0);
        found = this.statusDao.getAllFromUserOfType(this.user1, StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 1);

        IndexFragmentEntryStatus status = this.statusDao.getByUserAndDocumentUUID(this.user1, this.uuid1);
        assertNotNull(status);
        status = this.statusDao.getByUserAndDocumentUUID(this.user1, this.uuid3);
        assertNull(status);
    }

    @Test
    public void getAllByStatus() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByStatusType(StatusType.DELETED);
        assertTrue(found.size() == 1);
        found = this.statusDao.getAllByStatusType(StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 2);
        found = this.statusDao.getAllByStatusType(StatusType.IMPORTED);
        assertNotNull(found);
        assertTrue(found.size() == 0);
    }

    /**
     * Tests if the query returns document UUIDs that are within a given StatusType, filtered by user
     * 
     */
    @Test
    public void getAllByUserAndStatus() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllFromUserOfType(this.user1, StatusType.DELETED);
        assertTrue(found.size() == 0);
        found = this.statusDao.getAllFromUserOfType(this.user2, StatusType.DELETED);
        assertTrue(found.size() == 1);
    }

    /**
     * Tests if the query returns document UUIDs that are within one of the provided StatusTypes of a given user
     */
    @Test
    public void getAllByUserAnd1toNStatus() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllFromUserOfType(this.user2, StatusType.DELETED);
        assertTrue(found.size() == 1);
        found = this.statusDao.getAllFromUserInOneOfTheTypes(this.user2, StatusType.DELETED,
                StatusType.WAITING_FOR_DELETION);
        assertTrue(found.size() == 2);
    }

    /**
     * Tests if the query returns document UUIDs that are within one of the provided StatusTypes of a given user and
     * where the user is actually the document owner [not shared]
     */
    @Test
    public void getAllByUserAnd1toNStatusAndOwner() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllFromUserInOneOfTheTypesAndByUserAsDocumentOwner(
                this.user2, StatusType.DELETED);
        assertTrue(found.size() == 1);
    }

    /**
     * Tests if the query returns document UUIDs that are within one of the provided StatusTypes of a given user and
     * where the owner if actually a different user [sharing]
     */
    @Test
    public void getAllByUserAnd1toNStatusAndSharingPartner() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllFromUserInOneOfTheTypesAndByDocumentOwner(
                this.user3, this.user4, StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 1);
    }

    @Test
    public void getAllByDocumentUUID() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByDocumentUUID(this.uuid2);
        assertTrue(found.size() == 2);
        assertTrue(found.get(0).getId() < found.get(1).getId());
    }

    @Test
    public void getAllByBackupJobID() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByUserAndBackupJobID(this.user1, this.backupJobID1);
        assertTrue(found.size() == 1);

        found = this.statusDao.getAllByUserAndBackupJobID(this.user2, this.backupJobID2);
        assertTrue(found.size() == 0);

    }

    @Test
    public void getAllBeforeBackupDate() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByUserAndBeforeBackupDate(this.user1, this.dateNow);
        //contains elements with the query timestamp, expecting <= operation
        assertTrue(found.size() == 2);
        assertTrue(found.get(0).getId() < found.get(1).getId());
    }

    @Test
    public void getAllAfterBackupDate() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByUserAndAfterBackupDate(this.user1, this.dateNow);
        //contains elements with the query timestamp, expecting <= operation
        assertTrue(found.size() == 0);

        found = this.statusDao.getAllByUserAndAfterBackupDate(this.user2, this.dateNow);
        assertTrue(found.size() == 1);
    }

    @Test
    public void getAllByUserAndBeforeBackupDateAndByDocumentOwner() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByUserAndBeforeBackupDateAndByDocumentOwner(
                this.user1, this.user1, this.dateNow, StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 1);

        found = this.statusDao.getAllByUserOwnedAndBeforeBackupDate(this.user1, this.dateNow,
                StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 1);

        found = this.statusDao.getAllByUserAndBeforeBackupDateAndByDocumentOwner(this.user1, this.user1, this.dateNow,
                StatusType.WAITING_FOR_DELETION);
        assertTrue(found.size() == 1);
    }

    @Test
    public void getAllByUserAndAfterBackupDateAndByDocumentOwner() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByUserAndAfterBackupDateAndByDocumentOwner(
                this.user1, this.user1, this.dateNow, StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 0);

        found = this.statusDao.getAllByUserAndAfterBackupDateAndByDocumentOwner(this.user2, this.user2, this.dateNow,
                StatusType.WAITING_FOR_DELETION);
        assertTrue(found.size() == 1);

        found = this.statusDao.getAllByUserOwnedAndAfterBackupDate(this.user2, this.dateNow,
                StatusType.WAITING_FOR_DELETION);
        assertTrue(found.size() == 1);
    }

    @Test
    public void getAllByUserAndBackupJobIDandDocumentOwner() {
        persistTestData();
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByUserOwnedAndBackupJob(this.user1,
                this.backupJobID1, StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 1);

        found = this.statusDao.getAllByUserAndBackupJobAndByDocumentOwner(this.user3, this.user4, this.backupJobID1,
                StatusType.WAITING_FOR_IMPORT);
        assertTrue(found.size() == 1);
    }

    @Test
    public void getAllByUserAndDocumentUUIDByDocumentOwner() {
        persistTestData();
        IndexFragmentEntryStatus found = this.statusDao.getByUserAndDocumentUUIDByDocumentOwner(this.user1, this.user1,
                this.uuid1, StatusType.WAITING_FOR_IMPORT);
        assertNotNull(found);

        found = this.statusDao.getByUserOwnedAndDocumentUUID(this.user1, this.uuid1, StatusType.WAITING_FOR_IMPORT);
        assertNotNull(found);

        found = this.statusDao.getByUserAndDocumentUUIDByDocumentOwner(this.user3, this.user4, this.uuid4,
                StatusType.WAITING_FOR_IMPORT);
        assertNotNull(found);
    }

    @Test
    public void getAllByUserAndDocumentUUIDsByDocumentOwner() {
        persistTestData();
        List<UUID> l = new ArrayList<UUID>();
        l.add(this.uuid1);
        l.add(this.uuid2);
        List<IndexFragmentEntryStatus> found = this.statusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(this.user1,
                this.user1, l, StatusType.WAITING_FOR_IMPORT);
        assertNotNull(found);
        assertTrue("should find one entry", found.size() == 1);

        found = this.statusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(this.user1, this.user1, l,
                StatusType.WAITING_FOR_IMPORT, StatusType.WAITING_FOR_DELETION);
        assertNotNull(found);
        assertTrue("should find two entries", found.size() == 2);

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
        this.user3 = new User(3L);
        this.user4 = new User(4L);

        this.uuid1 = UUID.randomUUID();
        this.uuid2 = UUID.randomUUID();
        this.uuid3 = UUID.randomUUID();
        this.uuid4 = UUID.randomUUID();

        this.backupJobID1 = 98L;
        this.backupJobID2 = 99L;

        int hours = 2; //create a date in history and future
        this.dateAfterBackup = new Date(this.currentTime + hours * 60 * 60 * 1000);
        this.dateBeforeBackup = new Date(this.currentTime - hours * 60 * 60 * 1000);
        this.dateNow = new Date(this.currentTime);

        this.status1 = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT, this.uuid1, this.user1, this.user1,
                this.backupJobID1, this.dateNow);

        this.status2 = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_DELETION, this.uuid2, this.user1,
                this.user1, this.backupJobID2, this.dateBeforeBackup);

        this.status3 = new IndexFragmentEntryStatus(StatusType.DELETED, this.uuid3, this.user2, this.user2,
                this.backupJobID1, this.dateNow);

        this.status4 = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_DELETION, this.uuid2, this.user2,
                this.user2, this.backupJobID1, this.dateAfterBackup);

        this.status5 = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT, this.uuid4, this.user3, this.user4,
                this.backupJobID1, this.dateAfterBackup);
    }

    private void persistTestData() {
        this.status1 = persistInTransaction(this.status1);
        this.status2 = persistInTransaction(this.status2);
        this.status3 = persistInTransaction(this.status3);
        this.status4 = persistInTransaction(this.status4);
        this.status5 = persistInTransaction(this.status5);
    }

    private IndexFragmentEntryStatus persistInTransaction(IndexFragmentEntryStatus entryStatus) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        entryStatus = this.statusDao.save(entryStatus);
        this.database.entityManager.getTransaction().commit();
        return entryStatus;
    }

}
