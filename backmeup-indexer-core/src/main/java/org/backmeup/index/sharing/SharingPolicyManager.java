package org.backmeup.index.sharing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allows to set sharing policies e.g. User A shares IndexDocument with User B, User A shares Backup with User B, etc.
 *
 */
public class SharingPolicyManager {

    private Map<Long, ActiveSharingPoliciesForUser> sharingPolicies = new HashMap<Long, ActiveSharingPoliciesForUser>();
    private Map<String, Long> policyIDToUserIDMapping = new HashMap<String, Long>();

    public List<SharingPolicy> getAllPoliciesForUser(Long userID) {
        if (this.sharingPolicies.containsKey(userID)) {
            return this.sharingPolicies.get(userID).getPolicies();
        } else {
            return new ArrayList<SharingPolicy>();
        }
    }

    public SharingPolicy createSharingRule(Long ownerUserID, Long shareWithUserID, SharingPolicies policy) {
        SharingPolicy shPolicy = new SharingPolicy(ownerUserID, shareWithUserID, policy);
        shPolicy.setPolicyID(createPolicyKey());
        addPolicy(shPolicy);
        return shPolicy;
    }

    public void removeSharingRule(String sharingPolicyID) {
        if (this.policyIDToUserIDMapping.containsKey(sharingPolicyID)) {
            Long ownerUserID = this.policyIDToUserIDMapping.get(sharingPolicyID);
            this.sharingPolicies.get(ownerUserID).removePolicy(sharingPolicyID);
            this.policyIDToUserIDMapping.remove(sharingPolicyID);
        }
    }

    public void removeSharingPolicy(SharingPolicy p) {
        removeSharingRule(p.getPolicyID());
    }

    private void addPolicy(SharingPolicy p) {
        if (!this.sharingPolicies.containsKey(p.getFromUserID())) {
            ActiveSharingPoliciesForUser userPolicies = new ActiveSharingPoliciesForUser(p.getFromUserID());
            this.sharingPolicies.put(p.getFromUserID(), userPolicies);
        }
        this.policyIDToUserIDMapping.put(p.getPolicyID(), p.getFromUserID());
        this.sharingPolicies.get(p.getFromUserID()).addPolicy(p);
    }

    private String createPolicyKey() {
        //TODO switch to proper DB keys here
        return myRandomWithHigh(1, 100000) + "";
    }

    private static int myRandomWithHigh(int low, int high) {
        high++;
        return (int) (Math.random() * (high - low) + low);
    }
}
