package org.backmeup.index.query;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.IndexManager;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.dal.TaggedCollectionDao;
import org.backmeup.index.model.User;
import org.elasticsearch.client.Client;

/**
 * Start the instance and return the client
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@ApplicationScoped
public class ElasticSearchSetup {

    @Inject
    private IndexManager indexManager;
    @Inject
    private TaggedCollectionDao taggedCollectionDao;

    @SuppressWarnings("resource")
    // this is a factory method
    public IndexClient createIndexClient(User user) {
        Client elasticClient = startInstance(user);
        return createIndexClient(user, elasticClient);
    }

    private Client startInstance(User user) {
        return this.indexManager.initAndCreateAndDoEverthing(user);
    }

    private ElasticSearchIndexClient createIndexClient(User user, Client elasticClient) {
        return new ElasticSearchIndexClient(user, elasticClient, this.taggedCollectionDao);
    }
}
