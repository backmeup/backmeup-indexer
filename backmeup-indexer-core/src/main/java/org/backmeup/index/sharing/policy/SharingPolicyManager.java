package org.backmeup.index.sharing.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.backmeup.index.model.User;

/**
 * Allows to set sharing policies e.g. User A shares IndexDocument with User B, User A shares Backup with User B, etc.
 *
 */
public class SharingPolicyManager {

    private Map<Long, ActiveSharingPoliciesForUser> sharingPolicies = new HashMap<Long, ActiveSharingPoliciesForUser>();
    private Map<String, Long> policyIDToUserIDMapping = new HashMap<String, Long>();

    private static SharingPolicyManager instance;

    public static SharingPolicyManager getInstance() {
        if (instance == null) {
            synchronized (SharingPolicyManager.class) {
                instance = new SharingPolicyManager();
            }
        }
        return instance;
    }

    private SharingPolicyManager() {
        //
    }

    public List<SharingPolicy> getAllPoliciesForUser(User user) {
        if (this.sharingPolicies.containsKey(user.id())) {
            return this.sharingPolicies.get(user.id()).getPolicies();
        } else {
            return new ArrayList<SharingPolicy>();
        }
    }

    public SharingPolicy createSharingRule(User owner, User sharingWith, SharingPolicies policy) {
        SharingPolicy shPolicy = new SharingPolicy(owner, sharingWith, policy);
        shPolicy.setPolicyID(createPolicyKey());
        addPolicy(shPolicy);
        return shPolicy;
    }

    /**
     * @param owner
     * @param sharingWith
     * @param policy
     * @param sharedElementID
     *            either the IndexDocument UUID for SHARE_DOCUMENT or the BackupJobID for ShareBackupJob
     * @return
     */
    public SharingPolicy createSharingRule(User owner, User sharingWith, SharingPolicies policy, String sharedElementID) {
        SharingPolicy shPol = createSharingRule(owner, sharingWith, policy);
        shPol.setSharedElementID(sharedElementID);
        return shPol;
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
