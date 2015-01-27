package org.backmeup.index.sharing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Contains a List of SharingPolicies for a given User
 *
 */
public class ActiveSharingPoliciesForUser {

    private List<SharingPolicy> policies = new ArrayList<SharingPolicy>();
    private Long userID;

    public ActiveSharingPoliciesForUser(Long userID) {
        this.userID = userID;
    }

    public List<SharingPolicy> getPolicies() {
        return this.policies;
    }

    public void setPolicies(List<SharingPolicy> policies) {
        this.policies = policies;
    }

    public void addPolicy(SharingPolicy policy) {
        this.policies.add(policy);
    }

    public void removePolicy(SharingPolicy policy) {
        if (this.policies.contains(policy)) {
            this.policies.remove(policy);
        }
    }

    public void removePolicy(String policyID) {
        for (Iterator<SharingPolicy> iterator = this.policies.iterator(); iterator.hasNext();) {
            SharingPolicy p = iterator.next();
            if (p.getPolicyID().equals(policyID)) {
                //remove the current element from the iterator and the list
                iterator.remove();
            }
        }
    }

    public Long getUserID() {
        return this.userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }

}
