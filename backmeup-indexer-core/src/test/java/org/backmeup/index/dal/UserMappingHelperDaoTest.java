package org.backmeup.index.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.backmeup.index.utils.file.UserMappingHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the JPA Hibernate storage and retrieval layer for sharing policies via derby DB with
 * hibernate.hbm2ddl.auto=create
 */
public class UserMappingHelperDaoTest {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    private UserMappingHelperDao userMappingDao;

    private UserMappingHelper user1, user2, user3;

    @Before
    public void getDaoFromDb() {
        this.userMappingDao = this.database.userMappingHelperDao;
        createTestData();
    }

    @Test
    public void getByKeyserverID() {
        UserMappingHelper mapping = this.userMappingDao.getByKeyserverId(this.user1.getKsUserId());
        assertNotNull(mapping);
        assertEquals(this.user1.toString(), mapping.toString());
    }

    @Test
    public void getByBMUUserID() {
        UserMappingHelper mapping = this.userMappingDao.getByBMUUserId(this.user2.getBmuUserId());
        assertNotNull(mapping);
        assertEquals(this.user2.toString(), mapping.toString());
    }

    @Test
    public void updateValues() {
        UserMappingHelper user2x = new UserMappingHelper(2L, "K2x");
        persistInTransaction(user2x);
        UserMappingHelper mapping = this.userMappingDao.getByBMUUserId(2L);
        assertNotNull(mapping);
        assertEquals(user2x.toString(), mapping.toString());
    }

    private void persistInTransaction(UserMappingHelper userMapping) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.userMappingDao.save(userMapping);
        this.database.entityManager.getTransaction().commit();
    }

    private void mergeInTransaction(UserMappingHelper userMapping) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.userMappingDao.merge(userMapping);
        this.database.entityManager.getTransaction().commit();
    }

    private void createTestData() {
        this.user1 = new UserMappingHelper(1L, "K1");
        this.user2 = new UserMappingHelper(2L, "K2");
        this.user3 = new UserMappingHelper(3L, "K3");

        persistInTransaction(this.user1);
        persistInTransaction(this.user2);
        persistInTransaction(this.user3);
    }

}
