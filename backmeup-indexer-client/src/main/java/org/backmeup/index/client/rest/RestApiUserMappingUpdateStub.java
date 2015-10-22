package org.backmeup.index.client.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.backmeup.index.api.IndexerUserMappingServer;
import org.backmeup.index.client.IndexClientException;

/**
 * Remote stub of the RESTful user mapping update server component.
 * 
 */
public class RestApiUserMappingUpdateStub implements IndexerUserMappingServer {

    private final HttpMethods http = new HttpMethods();
    private final RestUrlsUserMappingHelper urls;

    public RestApiUserMappingUpdateStub(RestApiConfig config) {
        this.urls = new RestUrlsUserMappingHelper(config);
    }

    @Override
    public String updateUserMapping(Long bmuUserId, String keyserverUserId) {
        try {
            URI url = this.urls.forCreateUserMapping(bmuUserId, keyserverUserId);
            String body = this.http.post(url, "", 200);
            return body;
        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    private IndexClientException failedToContactServer(Exception problem) {
        return new IndexClientException("failed to contact user mapping server", problem);
    }
}
