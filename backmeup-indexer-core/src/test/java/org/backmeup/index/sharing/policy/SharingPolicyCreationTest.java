package org.backmeup.index.sharing.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.model.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

public class SharingPolicyCreationTest {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    private SharingPolicyManager shManager;
    private User owner = new User(1L);
    private User sharedWith = new User(2L);

    @Before
    public void before() {
        setupWhiteboxTest();
    }

    @Test
    public void createShareAllPolicy() throws InterruptedException {
        this.database.entityManager.getTransaction().begin();
        SharingPolicy p = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW);
        this.database.entityManager.getTransaction().commit();

        assertEquals(this.owner.id(), p.getFromUserID());
        assertEquals(this.sharedWith.id(), p.getWithUserID());
        assertNotNull(p.getId());
        assertEquals(SharingPolicies.SHARE_ALL_AFTER_NOW, p.getPolicy());
        //need to sleep as Date does not capture millis but just seconds
        Thread.sleep(1200);
        assertTrue(p.getPolicyCreationDate().before(new Date(System.currentTimeMillis())));
    }

    @Test
    public void addPolicyTwice() {
        this.database.entityManager.getTransaction().begin();
        SharingPolicy p = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW);
        this.database.entityManager.getTransaction().commit();

        assertEquals(this.owner.id(), p.getFromUserID());
        assertEquals(this.sharedWith.id(), p.getWithUserID());
        assertNotNull(p.getId());
        assertEquals(SharingPolicies.SHARE_ALL_AFTER_NOW, p.getPolicy());

        this.database.entityManager.getTransaction().begin();
        SharingPolicy p2 = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW);
        this.database.entityManager.getTransaction().commit();

        assertTrue(p.getId() == p2.getId());
        assertTrue(this.shManager.getAllActivePoliciesOwnedByUser(this.owner).size() == 1);

        this.database.entityManager.getTransaction().begin();
        SharingPolicy p3 = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW, "some name", "some description");
        this.database.entityManager.getTransaction().commit();

        assertTrue(
                "policies are treated as equal by their policyType, owner, partner and sharedElement, but ignore name and description",
                p.getId() == p3.getId());
        assertTrue(this.shManager.getAllActivePoliciesOwnedByUser(this.owner).size() == 1);
    }

    @Test
    public void addShareAllPolicyAndRemoveIt() {
        this.database.entityManager.getTransaction().begin();
        SharingPolicy p = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW);
        this.database.entityManager.getTransaction().commit();
        List<SharingPolicy> ps = this.shManager.getAllActivePoliciesOwnedByUser(this.owner);
        assertTrue(ps.contains(p));

        this.database.entityManager.getTransaction().begin();
        this.shManager.removeSharingPolicy(p.getId());
        this.database.entityManager.getTransaction().commit();
        ps = this.shManager.getAllActivePoliciesOwnedByUser(this.owner);
        assertTrue(ps.size() == 0);
    }

    @Test
    public void createShareBackupJobPolicy() {
        //Info: need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        //share all elements of backupJobID 1
        SharingPolicy p = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_BACKUP, "1", "My Name", "My Description");
        this.database.entityManager.getTransaction().commit();

        List<SharingPolicy> ps = this.shManager.getAllActivePoliciesOwnedByUser(this.owner);
        assertTrue(ps.contains(p));
        assertTrue(ps.get(0).getSharedElementID().equals("1"));
    }

    private void setupWhiteboxTest() {
        this.shManager = new SharingPolicyManager();
        Whitebox.setInternalState(this.shManager, "sharingPolicyDao", this.database.sharingPolicyDao);

    }

}
