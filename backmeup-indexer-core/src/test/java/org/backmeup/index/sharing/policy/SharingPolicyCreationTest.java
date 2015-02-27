package org.backmeup.index.sharing.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.backmeup.index.model.User;
import org.junit.Before;
import org.junit.Test;

public class SharingPolicyCreationTest {

    private SharingPolicyManager shManager;
    private User owner = new User(1L);
    private User sharedWith = new User(2L);

    @Before
    public void before() {
        this.shManager = SharingPolicyManager.getInstance();
        this.shManager.removeAllSharingPolicies();
    }

    @Test
    public void createShareAllPolicy() throws InterruptedException {

        SharingPolicy p = this.shManager.createSharingRule(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW);

        assertEquals(this.owner.id(), p.getFromUserID());
        assertEquals(this.sharedWith.id(), p.getWithUserID());
        assertNotNull(p.getPolicyID());
        assertEquals(SharingPolicies.SHARE_ALL_AFTER_NOW, p.getPolicy());
        //need to sleep as Date does not capture millis but just seconds
        Thread.sleep(1200);
        assertTrue(p.getPolicyCreationDate().before(new Date(System.currentTimeMillis())));
    }

    @Test
    public void addShareAllPolicyAndRemoveIt() {

        SharingPolicy p = this.shManager.createSharingRule(this.owner, this.sharedWith,
                SharingPolicies.SHARE_ALL_AFTER_NOW);
        List<SharingPolicy> ps = this.shManager.getAllPoliciesForUser(this.owner);
        assertTrue(ps.contains(p));

        this.shManager.removeSharingRule(p.getPolicyID());
        ps = this.shManager.getAllPoliciesForUser(this.owner);
        assertTrue(ps.size() == 0);
    }

    @Test
    public void createShareBackupJobPolicy() {
        SharingPolicy p = this.shManager.createSharingRule(this.owner, this.sharedWith, SharingPolicies.SHARE_BACKUP);
        //share all elements of backupJobID 1
        p.setSharedElementID("1");
        List<SharingPolicy> ps = this.shManager.getAllPoliciesForUser(this.owner);
        assertTrue(ps.contains(p));
        assertTrue(ps.get(0).getSharedElementID().equals("1"));
    }

}
