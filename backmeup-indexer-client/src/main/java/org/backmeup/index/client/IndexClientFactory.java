package org.backmeup.index.client;

public class IndexClientFactory {

    public IndexClient getIndexClient(Long userId) {
        return new ElasticSearchIndexClient(userId);
    }

}
