package org.backmeup.index.client.rest;

import java.io.IOException;

import org.backmeup.index.api.IndexerUserMappingClient;
import org.backmeup.index.api.IndexerUserMappingServer;
import org.backmeup.index.client.config.Configuration;

/**
 * Connects the local index user mapping client to the remote user mapping update server.
 * 
 */
public class RestApiUserMappingClient implements IndexerUserMappingClient {

    private final IndexerUserMappingServer server;

    public RestApiUserMappingClient() {
        this.server = new RestApiUserMappingStub(getRESTServerEndpointLocation());
    }

    private RestApiConfig getRESTServerEndpointLocation() {
        RestApiConfig config;
        String host = Configuration.getProperty("backmeup.indexer.rest.host");
        String port = Configuration.getProperty("backmeup.indexer.rest.port");
        String baseurl = Configuration.getProperty("backmeup.indexer.rest.baseurl");
        //check if a configuration was provided or if we're using the default config
        if ((host != null) && (port != null) && (baseurl != null)) {
            config = new RestApiConfig(host, Integer.valueOf(port), baseurl);
        } else {
            config = RestApiConfig.DEFAULT;
        }
        return config;
    }

    @Override
    public String updateUserMapping(Long bmuUserId, String keyserverUserId) throws IllegalArgumentException, IllegalStateException {
        return this.server.updateUserMapping(bmuUserId, keyserverUserId);
    }

    @Override
    public Long getBMUUserID(String keyserverUserId) throws IOException {
        return this.server.getBMUUserID(keyserverUserId);
    }

    @Override
    public String getKeyserverUserID(Long BMUUserId) throws IOException {
        return this.server.getKeyserverUserID(BMUUserId);
    }

    @Override
    public void close() {
    }

}
