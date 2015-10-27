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
public class RestApiUserMappingStub implements IndexerUserMappingServer {

    private final HttpMethods http = new HttpMethods();
    private final RestUrlsUserMappingHelper urls;

    public RestApiUserMappingStub(RestApiConfig config) {
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

    @Override
    public Long getBMUUserID(String keyserverUserId) {
        String body;
        try {
            URI url = this.urls.forGetBMUUserID(keyserverUserId);
            body = this.http.get(url, 200);
        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
        try {
            return Long.valueOf(body);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getKeyserverUserID(Long bmuUserID) {
        String body;
        try {
            URI url = this.urls.forGetKeyserverUserID(bmuUserID);
            body = this.http.get(url, 200);
            return body;
        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    private IndexClientException failedToContactServer(Exception problem) {
        return new IndexClientException("failed to contact user mapping server", problem);
    }

}
