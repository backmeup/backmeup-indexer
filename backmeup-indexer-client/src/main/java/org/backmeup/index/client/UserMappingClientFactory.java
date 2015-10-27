package org.backmeup.index.client;

import org.backmeup.index.api.IndexerUserMappingClient;
import org.backmeup.index.client.rest.RestApiUserMappingClient;

public class UserMappingClientFactory {

    public IndexerUserMappingClient getClient() {
        return new RestApiUserMappingClient();
    }

}
