package org.backmeup.index.sharing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class SharingPolicyCreationTest {

    SharingPolicyManager shManager;
    Long ownerUserID = 1L;
    Long shareWithUserID = 2L;

    @Before
    public void before() {
        this.shManager = new SharingPolicyManager();
    }

    @Test
    public void createSharingPolicy() {

        SharingPolicy p = this.shManager.createSharingRule(this.ownerUserID, this.shareWithUserID,
                SharingPolicies.SHARE_ALL);

        assertEquals(this.ownerUserID, p.getFromUserID());
        assertEquals(this.shareWithUserID, p.getWithUserID());
        assertNotNull(p.getPolicyID());
    }

    @Test
    public void addSharingPolicyAndRemoveIt() {

        SharingPolicy p = this.shManager.createSharingRule(this.ownerUserID, this.shareWithUserID,
                SharingPolicies.SHARE_ALL);
        List<SharingPolicy> ps = this.shManager.getAllPoliciesForUser(this.ownerUserID);
        assertTrue(ps.contains(p));

        this.shManager.removeSharingRule(p.getPolicyID());
        ps = this.shManager.getAllPoliciesForUser(this.ownerUserID);
        assertTrue(ps.size() == 0);
    }

}
