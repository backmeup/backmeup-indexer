package org.backmeup.index.model.sharing;

import java.util.Date;

import org.backmeup.index.model.User;

public class SharingPolicyEntry {

    public enum SharingPolicyTypeEntry {
        Document, DocumentGroup, Backup, AllFromNow, AllInklOld, TaggedCollection
    }

    private Long id;
    private Long fromUserID;
    private Long withUserID;
    private SharingPolicyTypeEntry policy;
    private String sharedElementID; //e.g. indexdocumentUUID or backupJobID dependent on policy
    private Date policyCreationDate;
    private String name;
    private String description;
    private int numberOfSharedDocuments; //number of documents that this policy has currently shared
    private boolean approvedBySharingpartner; //indicates if the sharing partner has already accepted it
    private Date policyLifeSpanStartDate; //start of life for this policy
    private Date policyLifeSpanEndDate; //end of life for this policy

    public SharingPolicyEntry(Long id, User fromUser, User withUser, SharingPolicyTypeEntry policy,
            Date policyCreationDate, String sharedElementID, String name, String description, int numberOfSharedDocs,
            boolean approvedBySharingpartner, Date lifespanStart, Date lifespanEnd) {
        this.id = id;
        this.fromUserID = fromUser.id();
        this.withUserID = withUser.id();
        this.policy = policy;
        this.policyCreationDate = policyCreationDate;
        this.sharedElementID = sharedElementID;
        this.name = name;
        this.description = description;
        this.numberOfSharedDocuments = numberOfSharedDocs;
        this.approvedBySharingpartner = approvedBySharingpartner;
        this.policyLifeSpanStartDate = lifespanStart;
        this.policyLifeSpanEndDate = lifespanEnd;
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
        return "id: '" + this.id + "', fromUserID: '" + this.fromUserID + "', withUserID: '" + this.withUserID
                + "', policy: '" + this.policy + "', sharedElement: '" + this.sharedElementID + "', name: '"
                + this.name + "', description: '" + this.description + "', numberOfSharedDocuments: '"
                + this.numberOfSharedDocuments + "', policytimespanstart: '" + this.policyLifeSpanStartDate.toString()
                + "', policytimespanend: '" + this.policyLifeSpanEndDate.toString() + "', approvedBySharingPartner: '"
                + this.approvedBySharingpartner + "'";
    }

    public Long getId() {
        return this.id;
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

    public int getNumberOfSharedDocuments() {
        return this.numberOfSharedDocuments;
    }

    public boolean getApprovedBySharingpartner() {
        return this.approvedBySharingpartner;
    }

    public Date getPolicyLifeSpanStartDate() {
        return this.policyLifeSpanStartDate;
    }

    public Date getPolicyLifeSpanEndDate() {
        return this.policyLifeSpanEndDate;
    }

}
