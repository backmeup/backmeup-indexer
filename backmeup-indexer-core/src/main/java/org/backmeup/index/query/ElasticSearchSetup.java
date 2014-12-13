package org.backmeup.index.query;

import java.util.concurrent.Callable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.IndexManager;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.dal.Transaction;
import org.backmeup.index.model.User;
import org.elasticsearch.client.Client;

/**
 * Start the instance and return the client. This is transactional.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@ApplicationScoped
public class ElasticSearchSetup {

    @Inject
    private IndexManager indexManager;
    @Inject
    private Transaction transaction;

    @SuppressWarnings("resource") // this is a factory method
    public IndexClient createIndexClient(User userId) {
        Client elasticClient = startInstance(userId);
        return createIndexClient(userId, elasticClient);
    }

    private Client startInstance(final User userId) {
        return transaction.inside(new Callable<Client>() {
            @Override
            public Client call() {
                return indexManager.initAndCreateAndDoEverthing(userId);
            }
        });
    }

    private ElasticSearchIndexClient createIndexClient(User userId, Client elasticClient) {
        return new ElasticSearchIndexClient(userId, elasticClient);
    }
}
