package org.backmeup.index.client.rest;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.backmeup.index.api.TaggedCollectionClient;
import org.backmeup.index.api.TaggedCollectionServer;
import org.backmeup.index.model.User;
import org.backmeup.index.model.tagging.TaggedCollectionEntry;

/**
 * Adapts the local sharing policy client to the remote sharing policy server.
 * 
 */
public class RestApiTaggedCollectionClient implements TaggedCollectionClient {

    private final TaggedCollectionServer server = new RestApiTaggedCollectionServerStub(RestApiConfig.DEFAULT);
    private final User currUser;

    public RestApiTaggedCollectionClient(User user) {
        this.currUser = user;
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollections() {
        return this.server.getAllTaggedCollections(this.currUser);
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollectionsByNameQuery(String query) {
        return this.server.getAllTaggedCollectionsByNameQuery(this.currUser, query);
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollectionsContainingDocuments(List<UUID> lDocumentUUIDs) {
        return this.server.getAllTaggedCollectionsContainingDocuments(this.currUser, lDocumentUUIDs);
    }

    @Override
    public String removeTaggedCollection(Long collectionID) {
        return this.server.removeTaggedCollection(this.currUser, collectionID);
    }

    @Override
    public TaggedCollectionEntry createAndAddTaggedCollection(String name, String description,
            List<UUID> containedDocumentIDs) {
        return this.server.createAndAddTaggedCollection(this.currUser, name, description, containedDocumentIDs);
    }

    @Override
    public String addDocumentsToTaggedCollection(Long collectionID, List<UUID> documentIDs) {
        return this.server.addDocumentsToTaggedCollection(this.currUser, collectionID, documentIDs);
    }

    @Override
    public String removeDocumentsFromTaggedCollection(Long collectionID, List<UUID> documentIDs) {
        return this.server.removeDocumentsFromTaggedCollection(this.currUser, collectionID, documentIDs);
    }

    @Override
    public String removeAllCollectionsForUser() {
        return this.server.removeAllCollectionsForUser(this.currUser);
    }

    @Override
    public void close() {
    }

}
