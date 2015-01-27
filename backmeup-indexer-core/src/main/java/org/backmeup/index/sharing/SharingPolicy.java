package org.backmeup.index.sharing;

public class SharingPolicy {

    private Long fromUserID;
    private Long withUserID;
    private SharingPolicies policy;
    private String policyID;
    private String sharedElementID; //e.g. indexdocumentUUID or backupJobID dependent on policy

    public SharingPolicy(Long fromUserID, Long withUserID, SharingPolicies policy) {
        this.fromUserID = fromUserID;
        this.withUserID = withUserID;
        this.policy = policy;
    }

    public Long getFromUserID() {
        return this.fromUserID;
    }

    public void setFromUserID(Long fromUserID) {
        this.fromUserID = fromUserID;
    }

    public Long getWithUserID() {
        return this.withUserID;
    }

    public void setWithUserID(Long withUserID) {
        this.withUserID = withUserID;
    }

    public SharingPolicies getPolicy() {
        return this.policy;
    }

    public void setPolicy(SharingPolicies policy) {
        this.policy = policy;
    }

    public String getPolicyID() {
        return this.policyID;
    }

    public void setPolicyID(String policyID) {
        this.policyID = policyID;
    }

    /**
     * e.g. indexdocumentUUID or backupJobID dependent on policy
     */
    public String getSharedElementID() {
        return this.sharedElementID;
    }

    public void setSharedElementID(String sharedElementID) {
        this.sharedElementID = sharedElementID;
    }

}
