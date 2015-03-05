package org.backmeup.index.sharing.policy;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.backmeup.index.model.User;

@Entity
public class SharingPolicy {

    @Id
    @GeneratedValue
    private Long Id;
    private Long fromUserID;
    private Long withUserID;
    @Enumerated(EnumType.STRING)
    private SharingPolicies policy;
    private String policyID;
    private String sharedElementID; //e.g. indexdocumentUUID or backupJobID dependent on policy
    @Temporal(TemporalType.TIMESTAMP)
    private Date policyCreationDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date policyLastCheckedDate;

    public SharingPolicy() {
    }

    public SharingPolicy(User fromUser, User withUser, SharingPolicies policy) {
        this.fromUserID = fromUser.id();
        this.withUserID = withUser.id();
        this.policy = policy;
        this.policyCreationDate = new Date();
        this.policyLastCheckedDate = null;
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

    @Override
    public String toString() {
        return "policyID: '" + this.policyID + "', fromUserID: '" + this.fromUserID + "', withUserID: '"
                + this.withUserID + "', policy: '" + this.policy + "', sharedElement: '" + this.sharedElementID + "'";
    }

    public Date getPolicyLastCheckedDate() {
        return this.policyLastCheckedDate;
    }

    public void setPolicyLastCheckedDate(Date policyLastCheckedDate) {
        this.policyLastCheckedDate = policyLastCheckedDate;
    }

}
