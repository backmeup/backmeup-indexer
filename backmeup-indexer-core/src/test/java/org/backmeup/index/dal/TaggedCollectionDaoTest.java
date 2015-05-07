package org.backmeup.index.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.backmeup.index.model.User;
import org.backmeup.index.tagging.TaggedCollection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the JPA Hibernate storage and retrieval layer for tagged collection via derby DB with
 * hibernate.hbm2ddl.auto=create
 */
public class TaggedCollectionDaoTest {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    private TaggedCollectionDao taggedColDao;

    private User user1, user2, user3;
    private TaggedCollection tag1, tag2, tag3, tag4;
    private UUID uuid1 = UUID.randomUUID();
    private UUID uuid2 = UUID.randomUUID();
    private UUID uuid3 = UUID.randomUUID();

    @Before
    public void getDaoFromDb() {
        this.taggedColDao = this.database.taggedColDao;
        createTestData();
    }

    @Test
    public void executeQueryAllFromUser() {
        List<TaggedCollection> found = this.taggedColDao.getAllFromUser(this.user1);
        assertNotNull(found);
        assertEquals(2, found.size());
        assertTrue(found.get(0).getId() == 1);

        found = this.taggedColDao.getAllFromUser(this.user2);
        assertNotNull(found);
        assertEquals(2, found.size());
    }

    @Test
    public void executeQueryForNonExistingUser() {
        List<TaggedCollection> found = this.taggedColDao.getAllFromUser(this.user3);
        assertNotNull(found);
        assertTrue(found.size() == 0);
    }

    @Test
    public void executeQueryByUserAndName() {
        List<TaggedCollection> found = this.taggedColDao.getAllFromUserAndName(this.user1, "collection 1üö");
        assertTrue(found.size() == 1);
        assertNotNull(found.get(0).getDocumentIds());

        found = this.taggedColDao.getAllFromUserAndName(this.user1, "collection");
        assertTrue(found.size() == 0);

        found = this.taggedColDao.getAllFromUserAndName(this.user1, null);
        assertTrue(found.size() == 0);

        found = this.taggedColDao.getAllFromUserAndName(this.user2, null);
        assertTrue(found.size() == 0);
    }

    @Test
    public void executeQueryByUserAndDocumentIDs() {
        List<TaggedCollection> found = this.taggedColDao.getAllFromUserContainingDocumentIds(this.user1,
                Arrays.asList(this.uuid2));
        assertTrue(found.size() == 1);

        found = this.taggedColDao.getAllFromUserContainingDocumentIds(this.user2, Arrays.asList(this.uuid1));
        assertTrue(found.size() == 2);

        found = this.taggedColDao
                .getAllFromUserContainingDocumentIds(this.user2, Arrays.asList(this.uuid1, this.uuid2));
        assertTrue(found.size() == 1);
    }

    @Test
    public void executeUpdateAddAndRemoveDocumentIDs() {
        //check we don't have this element defined yet
        List<TaggedCollection> found = this.taggedColDao.getAllFromUserContainingDocumentIds(this.user1,
                Arrays.asList(this.uuid2, this.uuid3));
        assertTrue(found.size() == 0);
        //get the element we want to update
        found = this.taggedColDao.getAllFromUserContainingDocumentIds(this.user1, Arrays.asList(this.uuid2));
        assertTrue(found.size() == 1);

        //test update elements
        TaggedCollection col = found.get(0);
        col.addDocumentId(this.uuid3);
        this.mergeInTransaction(col);

        //now we should be able to find the updated element
        found = this.taggedColDao
                .getAllFromUserContainingDocumentIds(this.user1, Arrays.asList(this.uuid2, this.uuid3));
        assertTrue(found.size() == 1);
        //and the original query should still be working
        found = this.taggedColDao.getAllFromUserContainingDocumentIds(this.user1, Arrays.asList(this.uuid2));
        assertTrue(found.size() == 1);

        //test element removal
        col = found.get(0);
        col.removeDocumentId(this.uuid3);
        this.mergeInTransaction(col);

        //now we should be able to find the updated element
        found = this.taggedColDao
                .getAllFromUserContainingDocumentIds(this.user1, Arrays.asList(this.uuid2, this.uuid3));
        assertTrue(found.size() == 0);
        //and the original query should still be working
        found = this.taggedColDao.getAllFromUserContainingDocumentIds(this.user1, Arrays.asList(this.uuid2));
        assertTrue(found.size() == 1);
    }

    private void persistInTransaction(TaggedCollection taggedColl) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.taggedColDao.save(taggedColl);
        this.database.entityManager.getTransaction().commit();
    }

    private void mergeInTransaction(TaggedCollection taggedColl) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.taggedColDao.merge(taggedColl);
        this.database.entityManager.getTransaction().commit();
    }

    private void createTestData() {
        this.user1 = new User(1L);
        this.user2 = new User(2L);
        this.user3 = new User(3L);

        this.tag1 = new TaggedCollection(this.user1, "collection 1üö", "description 1");
        this.tag2 = new TaggedCollection(this.user1, "collection 2", null, new ArrayList<UUID>(Arrays.asList(
                this.uuid1, this.uuid2)));
        this.tag3 = new TaggedCollection(this.user2, null, null, new ArrayList<UUID>(Arrays.asList(this.uuid1)));
        this.tag4 = new TaggedCollection(this.user2, null, null, new ArrayList<UUID>(Arrays.asList(this.uuid1,
                this.uuid2, this.uuid3)));

        persistInTransaction(this.tag1);
        persistInTransaction(this.tag2);
        persistInTransaction(this.tag3);
        persistInTransaction(this.tag4);
    }
}
