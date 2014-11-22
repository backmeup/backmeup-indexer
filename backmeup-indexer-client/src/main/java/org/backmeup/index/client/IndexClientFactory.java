package org.backmeup.index.client;

import org.backmeup.data.dummy.ElasticSearchIndexClient;
import org.backmeup.index.api.IndexClient;

public class IndexClientFactory {

    public IndexClient getIndexClient(Long userId) {
        return new ElasticSearchIndexClient(userId);
    }

}
