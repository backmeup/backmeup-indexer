package org.backmeup.index.tagging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.backmeup.index.model.User;

@Entity
public class TaggedCollection {

    public enum ActivityState {
        ACTIVE, //accepted by the sharing partner
        WAITING_FOR_DELETION, //waiting for items to get deleted
        DELETED; //deleted and no longer checked
    }

    @Id
    @GeneratedValue
    private Long Id;
    private Long userId;
    private String name;
    private String description;
    @Temporal(TemporalType.TIMESTAMP)
    private Date collectionCreationDate;
    @ElementCollection()
    @CollectionTable(name = "taggedcollection_documents", joinColumns = @JoinColumn(name = "id"))
    @Column(columnDefinition = "VARCHAR(39)")
    //@Type(type = "uuid-char")
    private List<String> documentIds = new ArrayList<String>();
    @Enumerated(EnumType.STRING)
    private ActivityState state;

    public TaggedCollection() {
    }

    public TaggedCollection(User userId, String name, String description, List<UUID> documentIDs) {
        this(userId, name, description);
        this.setDocumentIds(documentIDs);
    }

    public TaggedCollection(User userId, String name, String description) {
        this.userId = userId.id();
        this.collectionCreationDate = new Date();
        this.name = name;
        this.description = description;
        this.state = ActivityState.ACTIVE;
    }

    public Long getId() {
        return this.Id;
    }

    public void setId(Long id) {
        this.Id = id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Date getCollectionCreationDate() {
        return this.collectionCreationDate;
    }

    public void setCollectionCreationDate(Date collectionCreationDate) {
        this.collectionCreationDate = collectionCreationDate;
    }

    public List<UUID> getDocumentIds() {
        List<UUID> ret = new ArrayList<UUID>();
        for (String s : this.documentIds) {
            ret.add(UUID.fromString(s));
        }
        return ret;
    }

    public void setDocumentIds(List<UUID> documentIds) {
        this.documentIds = new ArrayList<String>();
        for (UUID uuid : documentIds) {
            this.documentIds.add(uuid.toString());
        }
    }

    public void addDocumentId(UUID documentId) {
        if (!this.documentIds.contains(documentId.toString())) {
            this.documentIds.add(documentId.toString());
        }
    }

    public void removeDocumentId(UUID documentId) {
        if (this.documentIds.contains(documentId.toString())) {
            this.documentIds.remove(documentId.toString());
        }
    }

    public ActivityState getState() {
        return this.state;
    }

    public void setState(ActivityState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "id: '" + this.Id + "', userId: '" + this.userId + "', name: '" + this.name + "', description: '"
                + this.description + "', creationDate: '" + this.collectionCreationDate.toString()
                + "', documentIds: '" + Arrays.toString(this.documentIds.toArray()) + "', state: '" + this.state + "'";
    }

}
