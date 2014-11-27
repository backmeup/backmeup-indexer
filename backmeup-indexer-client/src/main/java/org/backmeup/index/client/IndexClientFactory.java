package org.backmeup.index.client;

import org.backmeup.index.api.IndexClient;
import org.backmeup.index.client.rest.RestApiIndexClient;

public class IndexClientFactory {

    public IndexClient getIndexClient(Long userId) {
        return new RestApiIndexClient(userId);
    }

}
