package org.backmeup.index.client.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import org.apache.http.client.utils.URIBuilder;
import org.backmeup.index.model.User;

/**
 * Create the RESTful URLs to contact the tagged collection component.
 * 
 */
public class RestUrlsTaggedCollection {

    private final String host;
    private final int port;
    private final String basePath;

    public RestUrlsTaggedCollection(RestApiConfig config) {
        this.host = config.host;
        this.port = config.port;
        this.basePath = config.basepath + "/collections";
    }

    public URI forGetAllTaggedCollections(User user) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "");
        return urlBuilder.build();
    }

    public URI forGetAllTaggedCollectionsByNameQuery(User user, String query) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "");
        addMandatoryParameter(urlBuilder, "containsName", query);
        return urlBuilder.build();
    }

    public URI forGetAllTaggedCollectionsContainingDocuments(User user, List<UUID> lDocumentUUIDs)
            throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "");
        addMandatoryParameter(urlBuilder, "containsDocs", lDocumentUUIDs);
        return urlBuilder.build();
    }

    public URI forRemoveTaggedCollection(User user, Long collectionID) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "");
        addMandatoryParameter(urlBuilder, "collectionId", collectionID);
        return urlBuilder.build();
    }

    public URI forCreateAndAddTaggedCollection(User user, String name, String description,
            List<UUID> containedDocumentIDs) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "");
        addOptionalParameter(urlBuilder, "name", name);
        addOptionalParameter(urlBuilder, "description", description);
        addOptionalParameter(urlBuilder, "documentIds", containedDocumentIDs);

        return urlBuilder.build();
    }

    public URI forAddDocumentsToTaggedCollection(User user, Long collectionID, List<UUID> documentIDs)
            throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, collectionID + "/adddocuments");
        addMandatoryParameter(urlBuilder, "documentIds", documentIDs);
        return urlBuilder.build();
    }

    public URI forRemoveDocumentsFromTaggedCollection(User user, Long collectionID, List<UUID> documentIDs)
            throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, collectionID + "/removedocuments");
        addMandatoryParameter(urlBuilder, "documentIds", documentIDs);
        return urlBuilder.build();
    }

    public URI forRemoveAllCollectionsForUser(User user) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "");
        return urlBuilder.build();
    }

    // private

    private URIBuilder startWithBaseUrl(User userId, String path) throws URISyntaxException {
        return new URIBuilder("http://" + this.host + ":" + this.port + this.basePath + "/" + userId + "/" + path);
    }

    private void addMandatoryParameter(URIBuilder url, String key, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value);
    }

    private void addMandatoryParameter(URIBuilder url, String key, Long value) {
        if (value == null || value == 0) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value.toString());
    }

    private void addMandatoryParameter(URIBuilder url, String key, List<UUID> value) {
        if (value == null || value.size() < 1) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        for (UUID uuid : value) {
            url.addParameter(key, uuid.toString());
        }
    }

    private void addOptionalParameter(URIBuilder url, String key, String value) {
        if (value != null && !value.isEmpty()) {
            url.addParameter(key, value);
        }
    }

    private void addOptionalParameter(URIBuilder url, String key, List<UUID> value) {
        if (value != null && value.size() > 0) {
            for (UUID uuid : value) {
                url.addParameter(key, uuid.toString());
            }
        }
    }

}
