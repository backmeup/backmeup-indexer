package org.backmeup.index.sharing.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class SharingPolicyCreationTest {

    private SharingPolicyManager shManager;
    private Long ownerUserID = 1L;
    private Long shareWithUserID = 2L;

    @Before
    public void before() {
        this.shManager = SharingPolicyManager.getInstance();
    }

    @Test
    public void createShareAllPolicy() {

        SharingPolicy p = this.shManager.createSharingRule(this.ownerUserID, this.shareWithUserID,
                SharingPolicies.SHARE_ALL);

        assertEquals(this.ownerUserID, p.getFromUserID());
        assertEquals(this.shareWithUserID, p.getWithUserID());
        assertNotNull(p.getPolicyID());
        assertEquals(SharingPolicies.SHARE_ALL, p.getPolicy());
    }

    @Test
    public void addShareAllPolicyAndRemoveIt() {

        SharingPolicy p = this.shManager.createSharingRule(this.ownerUserID, this.shareWithUserID,
                SharingPolicies.SHARE_ALL);
        List<SharingPolicy> ps = this.shManager.getAllPoliciesForUser(this.ownerUserID);
        assertTrue(ps.contains(p));

        this.shManager.removeSharingRule(p.getPolicyID());
        ps = this.shManager.getAllPoliciesForUser(this.ownerUserID);
        assertTrue(ps.size() == 0);
    }

    @Test
    public void createShareBackupJobPolicy() {
        SharingPolicy p = this.shManager.createSharingRule(this.ownerUserID, this.shareWithUserID,
                SharingPolicies.SHARE_BACKUP);
        //share all elements of backupJobID 1
        p.setSharedElementID("1");
        List<SharingPolicy> ps = this.shManager.getAllPoliciesForUser(this.ownerUserID);
        assertTrue(ps.contains(p));
        assertTrue(ps.get(0).getSharedElementID().equals("1"));
    }

}
