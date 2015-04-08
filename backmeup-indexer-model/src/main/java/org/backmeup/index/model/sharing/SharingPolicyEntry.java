package org.backmeup.index.model.sharing;

import java.util.Date;

import org.backmeup.index.model.User;

public class SharingPolicyEntry {

    public enum SharingPolicyTypeEntry {
        Document, Backup, AllFromNow, AllInklOld
    }

    private Long Id;
    private Long fromUserID;
    private Long withUserID;
    private SharingPolicyTypeEntry policy;
    private String sharedElementID; //e.g. indexdocumentUUID or backupJobID dependent on policy
    private Date policyCreationDate;

    public SharingPolicyEntry(Long id, User fromUser, User withUser, SharingPolicyTypeEntry policy,
            Date policyCreationDate, String sharedElementID) {
        this.Id = id;
        this.fromUserID = fromUser.id();
        this.withUserID = withUser.id();
        this.policy = policy;
        this.policyCreationDate = policyCreationDate;
        this.sharedElementID = sharedElementID;
    }

    public Long getFromUserID() {
        return this.fromUserID;
    }

    public Long getWithUserID() {
        return this.withUserID;
    }

    public SharingPolicyTypeEntry getPolicy() {
        return this.policy;
    }

    /**
     * e.g. indexdocumentUUID or backupJobID dependent on policy
     */
    public String getSharedElementID() {
        return this.sharedElementID;
    }

    public Date getPolicyCreationDate() {
        return this.policyCreationDate;
    }

    @Override
    public String toString() {
        return "id: '" + this.Id + "', fromUserID: '" + this.fromUserID + "', withUserID: '" + this.withUserID
                + "', policy: '" + this.policy + "', sharedElement: '" + this.sharedElementID + "'";
    }

    public Long getId() {
        return this.Id;
    }

}
