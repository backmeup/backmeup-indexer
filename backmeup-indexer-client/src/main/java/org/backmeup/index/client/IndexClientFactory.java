package org.backmeup.index.client;

import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexDocumentUploadClient;
import org.backmeup.index.client.rest.RestApiIndexClient;
import org.backmeup.index.client.rest.RestApiIndexDocumentUploadClient;
import org.backmeup.index.model.User;

public class IndexClientFactory {

    public IndexClient getIndexClient(Long userId) {
        return getIndexClient(new User(userId));
    }

    public IndexClient getIndexClient(User user) {
        return new RestApiIndexClient(user);
    }

    public IndexDocumentUploadClient getIndexDocumentUploadClient(Long userId) {
        return getIndexDocumentUploadClient(new User(userId));
    }

    public IndexDocumentUploadClient getIndexDocumentUploadClient(User user) {
        return new RestApiIndexDocumentUploadClient(user);
    }

}
