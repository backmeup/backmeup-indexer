package org.backmeup.index.client;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IndexClientFactory {

    public IndexClient getIndexClient(Long userId) {
        return new ElasticSearchIndexClient(userId);
    }

}
