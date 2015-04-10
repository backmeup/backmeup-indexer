package org.backmeup.index.client.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.backmeup.index.api.IndexDocumentUploadServer;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;

/**
 * Remote stub of the RESTful index document upload server component.
 * 
 */
public class RestApiIndexDocumentUploadServerStub implements IndexDocumentUploadServer {

    private final HttpMethods http = new HttpMethods();
    private final RestUrlsIndexDocumentUpload urls;

    public RestApiIndexDocumentUploadServerStub(RestApiConfig config) {
        this.urls = new RestUrlsIndexDocumentUpload(config);
    }

    @Override
    public String uploadForSharing(User currUser, IndexDocument document) throws IOException {
        try {
            URI url = this.urls.forUploadForSharing(currUser);
            String jsonPayload = Json.serialize(document);
            String body = this.http.post(url, jsonPayload, 201);
            return body;
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
