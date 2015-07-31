package org.backmeup.index.sharing.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicy.ActivityState;
import org.junit.Before;
import org.junit.Ignore;
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
        assertTrue(p.getPolicyLifeSpanStartDate().before(p.getPolicyLifeSpanEndDate()));
        assertEquals(SharingPolicies.SHARE_ALL_AFTER_NOW, p.getPolicy());
        //need to sleep as Date does not capture millis but just seconds
        Thread.sleep(1200);
        assertTrue(p.getPolicyCreationDate().before(new Date(System.currentTimeMillis())));
    }

    @Test
    public void updateSharingPolicyMetadataAndLifespan() throws InterruptedException {
        String policyName = "my policy name";
        String policyDescription = "my policy description";
        this.database.entityManager.getTransaction().begin();
        SharingPolicy p = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW, policyName, policyDescription);
        this.database.entityManager.getTransaction().commit();

        assertEquals(this.owner.id(), p.getFromUserID());
        assertEquals(this.sharedWith.id(), p.getWithUserID());
        assertNotNull(p.getId());
        assertNotNull(p.getName().equals(policyName));
        assertNotNull(p.getDescription().equals(policyDescription));
        assertNotNull(p.getPolicyLifeSpanStartDate());
        assertNotNull(p.getPolicyLifeSpanEndDate());
        assertEquals(SharingPolicies.SHARE_ALL_AFTER_NOW, p.getPolicy());
        //need to sleep as Date does not capture millis but just seconds
        Thread.sleep(1200);
        assertTrue(p.getPolicyCreationDate().before(new Date(System.currentTimeMillis())));

        //now update the policy
        policyName = "my policy name2";
        Date pStartDate = new Date();
        Thread.sleep(1200);
        Date pEndDate = new Date();
        this.database.entityManager.getTransaction().begin();
        p = this.shManager.updateSharingPolicy(this.owner, p.getId(), policyName, null, pStartDate, pEndDate);
        this.database.entityManager.getTransaction().commit();

        assertEquals(this.owner.id(), p.getFromUserID());
        assertEquals(this.sharedWith.id(), p.getWithUserID());
        assertNotNull(p.getId());
        assertNotNull(p.getName().equals(policyName));
        assertNotNull(p.getName().equals(policyDescription));
        assertNotNull(p.getPolicyLifeSpanStartDate().equals(pStartDate));
        assertNotNull(p.getPolicyLifeSpanEndDate().equals(pEndDate));
        assertEquals(SharingPolicies.SHARE_ALL_AFTER_NOW, p.getPolicy());
    }

    @Test
    @Ignore("Items can now be created more than once - not checked for duplicates")
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
    public void testActivityState() {
        this.database.entityManager.getTransaction().begin();
        SharingPolicy p = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW);
        assertEquals(ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE, p.getState());
        this.database.entityManager.getTransaction().commit();

        List<SharingPolicy> ps = this.shManager.getAllActivePoliciesOwnedByUser(this.owner);
        assertFalse(ps.contains(p));

        this.database.entityManager.getTransaction().begin();
        this.shManager.approveIncomingSharing(this.sharedWith, p.getId());
        this.database.entityManager.getTransaction().commit();

        ps = this.shManager.getAllActivePoliciesOwnedByUser(this.owner);
        assertTrue(ps.contains(p));
    }

    @Test
    public void addShareAllPolicyAndRemoveIt() {
        this.database.entityManager.getTransaction().begin();
        SharingPolicy p = this.shManager.createAndAddSharingPolicy(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW);
        this.shManager.approveIncomingSharing(this.sharedWith, p.getId());
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
        this.shManager.approveIncomingSharing(this.sharedWith, p.getId());
        this.database.entityManager.getTransaction().commit();

        List<SharingPolicy> ps = this.shManager.getAllActivePoliciesOwnedByUser(this.owner);
        assertTrue(ps.contains(p));
        assertTrue(ps.get(0).getSharedElementID().equals("1"));
    }

    @Test
    public void testUUIDSerealizationOfSharedElementForPolicyShareDocumentGroup() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<UUID> lUUIDs = new ArrayList<UUID>();
        lUUIDs.add(uuid1);
        lUUIDs.add(uuid2);
        String sharedElementID = lUUIDs.toString();
        try {
            String[] sArr = sharedElementID.substring(1, sharedElementID.length() - 1).split(",\\s*");
            List<String> lArr = Arrays.asList(sArr);
            assertTrue(lArr.size() == 2);
            //test records
            assertEquals(uuid1, UUID.fromString(lArr.get(0)));
            assertEquals(uuid2, UUID.fromString(lArr.get(1)));
        } catch (Exception e) {
            assertTrue(e.toString(), false);
        }
    }

    private void setupWhiteboxTest() {
        this.shManager = new SharingPolicyManager();
        Whitebox.setInternalState(this.shManager, "sharingPolicyDao", this.database.sharingPolicyDao);

    }

}
