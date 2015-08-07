package org.backmeup.index.dal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the JPA Hibernate storage and retrieval layer for sharing policies via derby DB with
 * hibernate.hbm2ddl.auto=create
 */
public class SharingPolicyDaoTest {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    private SharingPolicyDao sharingPolicyDao;
    private HeritagePolicyDao heritagePolicyDao;

    private User user1, user2, user3;
    private SharingPolicy pol1, pol2;
    private UUID uuid1 = UUID.randomUUID();

    @Before
    public void getDaoFromDb() {
        this.sharingPolicyDao = this.database.sharingPolicyDao;
        this.heritagePolicyDao = this.database.heritagePolicyDao;
        createTestData();
    }

    @Test
    public void shouldStorePolicyAndReadAllFromDB() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPolicies();
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 2);

        lPolicies = this.heritagePolicyDao.getAllHeritagePolicies();
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    @Test
    public void getOutgoingPoliciesForAUser() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUser(this.user1);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 2);

        lPolicies = this.heritagePolicyDao.getAllHeritagePoliciesFromUser(this.user1);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    @Test
    public void getOutgoingPoliciesForUserWaitingForDeletion() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUser(this.user1);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 2);

        SharingPolicy p1 = lPolicies.get(0);
        p1.setState(ActivityState.WAITING_FOR_DELETION);
        this.mergeInTransaction(p1);

        lPolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUser(this.user1);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 2);

        lPolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUserInState(this.user1,
                ActivityState.WAITING_FOR_DELETION);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    @Test
    public void getIncomingPoliciesForAUser() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesWithUser(this.user2);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        lPolicies = this.heritagePolicyDao.getAllHeritagePoliciesWithUser(this.user2);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    @Test
    public void getPoliciesBetweenTwoUser() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsers(this.user1, this.user2);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        lPolicies = this.heritagePolicyDao.getAllHeritagePoliciesBetweenUsers(this.user1, this.user2);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    @Test
    public void getPoliciesBetweenTwoUserWaitingForDeletion() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsers(this.user1, this.user2);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        SharingPolicy p1 = lPolicies.get(0);
        p1.setState(ActivityState.WAITING_FOR_DELETION);
        this.mergeInTransaction(p1);

        lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsers(this.user1, this.user2);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUserInState(this.user1, this.user2,
                ActivityState.WAITING_FOR_DELETION);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    @Test
    public void getNotExistingPolicy() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsers(this.user2, this.user3);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 0);
    }

    @Test
    public void getSharingPolicyOfSpecificType() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsersInType(this.user1,
                this.user2, SharingPolicies.SHARE_ALL_AFTER_NOW);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsersInType(this.user1, this.user2,
                SharingPolicies.SHARE_ALL_AFTER_NOW, SharingPolicies.SHARE_ALL_INKLUDING_OLD,
                SharingPolicies.SHARE_INDEX_DOCUMENT);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsersInType(this.user2, this.user3,
                SharingPolicies.SHARE_ALL_AFTER_NOW, SharingPolicies.SHARE_INDEX_DOCUMENT);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 0);
    }

    @Test
    public void getSharingPolicyOfSpecificTypeAndStateForOwner() {
        //check the entry state for this test
        List<SharingPolicy> lPolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUserInStateAndOfType(this.user1,
                SharingPolicies.SHARE_TAGGED_COLLECTION, ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 0);

        //now change a policy to match this policy type
        lPolicies = this.sharingPolicyDao.getAllSharingPoliciesBetweenUsersInType(this.user1, this.user2,
                SharingPolicies.SHARE_ALL_AFTER_NOW, SharingPolicies.SHARE_ALL_INKLUDING_OLD,
                SharingPolicies.SHARE_INDEX_DOCUMENT);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        SharingPolicy p = lPolicies.get(0);
        p.setPolicy(SharingPolicies.SHARE_TAGGED_COLLECTION);
        this.mergeInTransaction(p);

        //now we should be able to find it
        lPolicies = this.sharingPolicyDao.getAllSharingPoliciesFromUserInStateAndOfType(this.user1,
                SharingPolicies.SHARE_TAGGED_COLLECTION, ActivityState.HERITAGE_WAITING_FOR_ACTIVATION);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        lPolicies = this.heritagePolicyDao.getAllHeritagePoliciesFromUser(this.user1);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    @Test
    public void getSharingPolicyOfSpecificTypeOverAllUsers() {
        //check the entry state for this test
        List<SharingPolicy> lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        SharingPolicy p = lPolicies.get(0);
        p.setState(ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START);
        this.mergeInTransaction(p);

        lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 0);

        lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    @Test
    public void getAllHeritagePoliciesForDeadManSwitch() {
        List<SharingPolicy> lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 0);
        lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.HERITAGE_WAITING_FOR_ACTIVATION);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        lPolicies = this.heritagePolicyDao.getAllHeritagePoliciesBetweenUsers(this.user1, this.user2);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);

        //now activate the heritage - marks elements for import
        this.database.entityManager.getTransaction().begin();
        this.heritagePolicyDao.acceptAndActivateHeritage(this.user2);
        this.database.entityManager.getTransaction().commit();

        lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.ACCEPTED_AND_WAITING_FOR_TIMSPAN_START);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
        lPolicies = this.sharingPolicyDao
                .getAllSharingPoliciesOverAllUsersInState(ActivityState.HERITAGE_WAITING_FOR_ACTIVATION);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 0);

        //heritage policy is still available
        lPolicies = this.heritagePolicyDao.getAllHeritagePoliciesBetweenUsers(this.user1, this.user2);
        assertNotNull(lPolicies);
        assertTrue(lPolicies.size() == 1);
    }

    private void persistInTransaction(SharingPolicy policy) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.sharingPolicyDao.save(policy);
        this.database.entityManager.getTransaction().commit();
    }

    private void mergeInTransaction(SharingPolicy policy) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.sharingPolicyDao.merge(policy);
        this.database.entityManager.getTransaction().commit();
    }

    private void createTestData() {
        this.user1 = new User(1L);
        this.user2 = new User(2L);
        this.user3 = new User(3L);

        this.pol1 = new SharingPolicy(this.user1, this.user2, SharingPolicies.SHARE_ALL_AFTER_NOW, "MyHeritage1",
                "Heritage1");
        //init policy as heritage policy
        this.pol1.initHeritagePolicy();
        this.pol2 = new SharingPolicy(this.user1, this.user3, SharingPolicies.SHARE_INDEX_DOCUMENT, "MyPolicy2",
                "Description2");
        this.pol2.setSharedElementID(this.uuid1.toString());

        persistInTransaction(this.pol1);
        persistInTransaction(this.pol2);
    }

}
