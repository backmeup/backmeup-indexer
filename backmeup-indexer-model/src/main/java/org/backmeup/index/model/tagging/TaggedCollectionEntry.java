package org.backmeup.index.model.tagging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.backmeup.index.model.User;

public class TaggedCollectionEntry {

    private Long Id;
    private Long userId;
    private String name;
    private String description;
    private Date collectionCreationDate;
    private List<UUID> documentIds = new ArrayList<UUID>();
    //number of documents in this collection that are currently actually imported/available in the users ES index
    private int numberOfActuallyAvailableDocuments;

    public TaggedCollectionEntry(Long Id, User user, String name, String description, Date collectionCreationDate,
            List<UUID> collection, int numberOfActuallyAvailableDocuments) {
        this.Id = Id;
        this.userId = user.id();
        this.name = name;
        this.description = description;
        this.collectionCreationDate = collectionCreationDate;
        this.documentIds = collection;
        this.numberOfActuallyAvailableDocuments = numberOfActuallyAvailableDocuments;
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
        return this.documentIds;
    }

    public void setDocumentIds(List<UUID> documentIds) {
        this.documentIds = documentIds;
    }

    public int getNumberOfActuallyAvailableDocuments() {
        return this.numberOfActuallyAvailableDocuments;
    }

    public void setNumberOfActuallyAvailableDocuments(int numberOfActuallyAvailableDocuments) {
        this.numberOfActuallyAvailableDocuments = numberOfActuallyAvailableDocuments;
    }

    @Override
    public String toString() {
        return "id: '" + this.Id + "', userId: '" + this.userId + "', name: '" + this.name + "', description: '"
                + this.description + "', creationDate: '" + this.collectionCreationDate.toString()
                + "', documentIds: '" + Arrays.toString(this.documentIds.toArray())
                + "', numberOfActuallyAvailableDocuments: '" + this.numberOfActuallyAvailableDocuments + "'";
    }

}
