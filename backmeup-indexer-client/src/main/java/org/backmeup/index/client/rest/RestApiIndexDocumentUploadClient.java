package org.backmeup.index.client.rest;

import java.io.IOException;

import org.backmeup.index.api.IndexDocumentUploadClient;
import org.backmeup.index.api.IndexDocumentUploadServer;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;

/**
 * Adapts the local index document upload client to the remote index document upload server.
 * 
 */
public class RestApiIndexDocumentUploadClient implements IndexDocumentUploadClient {

    private final IndexDocumentUploadServer server = new RestApiIndexDocumentUploadServerStub(RestApiConfig.DEFAULT);
    private final User currUser;

    public RestApiIndexDocumentUploadClient(User currUser) {
        this.currUser = currUser;
    }

    @Override
    public String uploadForSharing(IndexDocument document) throws IOException {
        return this.server.uploadForSharing(this.currUser, document);
    }

    @Override
    public void close() {
    }

}
