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
    private String sharedElementID; //e.g. indexdocumentUUID or backupJobID dependent on policy
    @Temporal(TemporalType.TIMESTAMP)
    private Date policyCreationDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date policyLastCheckedDate;
    private String name;
    private String description;

    public SharingPolicy() {
    }

    public SharingPolicy(User fromUser, User withUser, SharingPolicies policy, String sharedElementID, String name,
            String description) {
        this(fromUser, withUser, policy, name, description);
        this.sharedElementID = sharedElementID;
    }

    public SharingPolicy(User fromUser, User withUser, SharingPolicies policy, String name, String description) {
        this.fromUserID = fromUser.id();
        this.withUserID = withUser.id();
        this.policy = policy;
        this.policyCreationDate = new Date();
        this.policyLastCheckedDate = null;
        this.name = name;
        this.description = description;
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
        return "id: '" + this.Id + "', fromUserID: '" + this.fromUserID + "', withUserID: '" + this.withUserID
                + "', policy: '" + this.policy + "', sharedElement: '" + this.sharedElementID + "'";
    }

    public Date getPolicyLastCheckedDate() {
        return this.policyLastCheckedDate;
    }

    public void setPolicyLastCheckedDate(Date policyLastCheckedDate) {
        this.policyLastCheckedDate = policyLastCheckedDate;
    }

    public Long getId() {
        return this.Id;
    }

    public void setId(Long id) {
        this.Id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
