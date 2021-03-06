package org.backmeup.index.sharing.policy;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
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

    public enum ActivityState {
        CREATED_AND_WAITING_FOR_HANDSHAKE, //after created by the owner
        ACCEPTED_AND_WAITING_FOR_TIMSPAN_START, //accepted by sharing partner and waiting for policy start date
        ACCEPTED_AND_ACTIVE, //accepted by the sharing partner and active
        WAITING_FOR_DELETION, //waiting for items to get deleted
        DELETED, //deleted and no longer checked
        HERITAGE_WAITING_FOR_ACTIVATION; //status for policies representing the use case 'Vererben'
    }

    public enum Type {
        SHARING, //standard sharing between two users
        HERITAGE; //heritage sharing for use case 'vererben'
    }

    @Id
    @GeneratedValue
    private Long Id;
    private Long fromUserID;
    private Long withUserID;
    @Enumerated(EnumType.STRING)
    private SharingPolicies policy;
    @Column(length = 4000)
    private String sharedElementID; //e.g. indexdocumentUUID or backupJobID dependent on policy
    @Temporal(TemporalType.TIMESTAMP)
    private Date policyCreationDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date policyLastCheckedDate;
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    private ActivityState state;
    @Temporal(TemporalType.TIMESTAMP)
    private Date policyLifeSpanStartDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date policyLifeSpanEndDate;
    @Enumerated(EnumType.STRING)
    private Type type;

    public SharingPolicy() {
    }

    public SharingPolicy(User fromUser, User withUser, SharingPolicies policy, String sharedElementID, String name,
            String description) {
        this(fromUser, withUser, policy, name, description);
        this.sharedElementID = sharedElementID;
    }

    public SharingPolicy(User fromUser, User withUser, SharingPolicies policy, String name, String description) {
        this(fromUser, withUser, policy, name, description, Type.SHARING); //policies default to sharing
    }

    public SharingPolicy(User fromUser, User withUser, SharingPolicies policy, String name, String description,
            Type type) {
        this.fromUserID = fromUser.id();
        this.withUserID = withUser.id();
        this.policy = policy;
        this.policyCreationDate = new Date();
        this.policyLifeSpanStartDate = getDefaultLifeSpanStartDate(); //defaults to current Date
        this.policyLifeSpanEndDate = getDefaultLifeSpanEndDate(); //defaults to 31.12.2999
        this.policyLastCheckedDate = null;
        this.name = name;
        this.description = description;
        this.state = ActivityState.CREATED_AND_WAITING_FOR_HANDSHAKE;
        if (type != null) {
            this.type = type;
        } else {
            this.type = Type.SHARING; //policies default to sharing
        }

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

    @Deprecated
    public Date getPolicyLastCheckedDate() {
        return this.policyLastCheckedDate;
    }

    @Deprecated
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

    public ActivityState getState() {
        return this.state;
    }

    public void setState(ActivityState state) {
        this.state = state;
    }

    public static Date getDefaultLifeSpanStartDate() {
        Date date = new Date();
        return date;
    }

    public static Date getDefaultLifeSpanEndDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.DAY_OF_MONTH, 31);
        calendar.set(Calendar.MONTH, 12);
        calendar.set(Calendar.YEAR, 2999);
        Date date = calendar.getTime();
        return date;
    }

    public Date getPolicyLifeSpanStartDate() {
        return this.policyLifeSpanStartDate;
    }

    public void setPolicyLifeSpanStartDate(Date policyLifeSpanStartDate) {
        //only allow dates before the end date of the policy
        if (policyLifeSpanStartDate.before(this.policyLifeSpanEndDate)) {
            this.policyLifeSpanStartDate = policyLifeSpanStartDate;
        }
    }

    public Date getPolicyLifeSpanEndDate() {
        return this.policyLifeSpanEndDate;
    }

    public void setPolicyLifeSpanEndDate(Date policyLifeSpanEndDate) {
        //only allow dates after the policy start date
        if (policyLifeSpanEndDate.after(this.policyLifeSpanStartDate)) {
            this.policyLifeSpanEndDate = policyLifeSpanEndDate;
        }
    }

    public Type getType() {
        return this.type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void initHeritagePolicy() {
        this.type = Type.HERITAGE;
        this.state = ActivityState.HERITAGE_WAITING_FOR_ACTIVATION;
    }

    @Override
    public String toString() {
        return "id: '" + this.Id + "', fromUserID: '" + this.fromUserID + "', withUserID: '" + this.withUserID
                + "', policy: '" + this.policy + "', sharedElement: '" + this.sharedElementID + "', name: '"
                + this.name + "', description: '" + this.description + "', type: '" + this.type + "', creationDate: '"
                + this.policyCreationDate.toString() + "', lifespanStartDate: '"
                + this.policyLifeSpanStartDate.toString() + "', lifespanEndDate: '"
                + this.policyLifeSpanEndDate.toString() + "'";
    }

}
