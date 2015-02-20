package org.backmeup.index.sharing.policy;

import java.util.Date;

import org.backmeup.index.model.User;

public class SharingPolicy {

    private Long fromUserID;
    private Long withUserID;
    private SharingPolicies policy;
    private String policyID;
    private String sharedElementID; //e.g. indexdocumentUUID or backupJobID dependent on policy
    private Date policyCreationDate;

    public SharingPolicy(User fromUser, User withUser, SharingPolicies policy) {
        this.fromUserID = fromUser.id();
        this.withUserID = withUser.id();
        this.policy = policy;
        this.policyCreationDate = new Date(System.currentTimeMillis());
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

    public Date getPolicyCreationDate() {
        return this.policyCreationDate;
    }

    public void setPolicyCreationDate(Date policyCreationDate) {
        this.policyCreationDate = policyCreationDate;
    }

}
