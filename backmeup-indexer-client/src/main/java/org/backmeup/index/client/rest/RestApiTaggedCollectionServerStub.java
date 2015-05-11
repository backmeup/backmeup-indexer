package org.backmeup.index.client.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.backmeup.index.api.TaggedCollectionServer;
import org.backmeup.index.client.IndexClientException;
import org.backmeup.index.model.User;
import org.backmeup.index.model.tagging.TaggedCollectionEntry;
import org.backmeup.index.serializer.Json;

/**
 * Remote stub of the RESTful sharing policy component.
 * 
 */
public class RestApiTaggedCollectionServerStub implements TaggedCollectionServer {

    private final HttpMethods http = new HttpMethods();
    private final RestUrlsTaggedCollection urls;

    public RestApiTaggedCollectionServerStub(RestApiConfig config) {
        this.urls = new RestUrlsTaggedCollection(config);
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollections(User user) {
        try {
            URI url = this.urls.forGetAllTaggedCollections(user);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfTaggedCollectionEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollectionsByNameQuery(User user, String query) {
        try {
            URI url = this.urls.forGetAllTaggedCollectionsByNameQuery(user, query);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfTaggedCollectionEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public Set<TaggedCollectionEntry> getAllTaggedCollectionsContainingDocuments(User user, List<UUID> lDocumentUUIDs) {
        try {
            URI url = this.urls.forGetAllTaggedCollectionsContainingDocuments(user, lDocumentUUIDs);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfTaggedCollectionEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String removeTaggedCollection(User user, Long collectionID) {
        try {
            URI url = this.urls.forRemoveTaggedCollection(user, collectionID);
            String body = this.http.delete(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public TaggedCollectionEntry createAndAddTaggedCollection(User user, String name, String description,
            List<UUID> containedDocumentIDs) {
        try {
            URI url = this.urls.forCreateAndAddTaggedCollection(user, name, description, containedDocumentIDs);
            String body = this.http.post(url, "", 200);
            return Json.deserialize(body, TaggedCollectionEntry.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String addDocumentsToTaggedCollection(User user, Long collectionID, List<UUID> documentIDs) {
        try {
            URI url = this.urls.forAddDocumentsToTaggedCollection(user, collectionID, documentIDs);
            String body = this.http.post(url, "", 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String removeDocumentsFromTaggedCollection(User user, Long collectionID, List<UUID> documentIDs) {
        try {
            URI url = this.urls.forAddDocumentsToTaggedCollection(user, collectionID, documentIDs);
            String body = this.http.delete(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String removeAllCollectionsForUser(User user) {
        try {
            URI url = this.urls.forRemoveAllCollectionsForUser(user);
            String body = this.http.delete(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    private IndexClientException failedToContactServer(Exception problem) {
        return new IndexClientException("faled to contact tagged document management server", problem);
    }
}
