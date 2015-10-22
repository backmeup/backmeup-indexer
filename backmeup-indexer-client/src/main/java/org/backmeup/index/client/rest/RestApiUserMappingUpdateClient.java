package org.backmeup.index.client.rest;

import org.backmeup.index.api.IndexerUserMappingClient;
import org.backmeup.index.api.IndexerUserMappingServer;

/**
 * Connects the local index user mapping client to the remote user mapping update server.
 * 
 */
public class RestApiUserMappingUpdateClient implements IndexerUserMappingClient {

    private final IndexerUserMappingServer server = new RestApiUserMappingUpdateStub(RestApiConfig.DEFAULT);

    @Override
    public String updateUserMapping(Long bmuUserId, String keyserverUserId) throws IllegalArgumentException, IllegalStateException {
        return this.server.updateUserMapping(bmuUserId, keyserverUserId);
    }

    @Override
    public void close() {
    }

}
