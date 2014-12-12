package org.backmeup.index.client;

import org.backmeup.index.api.IndexClient;
import org.backmeup.index.client.rest.RestApiIndexClient;
import org.backmeup.index.model.User;

public class IndexClientFactory {

    public IndexClient getIndexClient(Long userId) {
        return getIndexClient(new User(userId));
    }

    public IndexClient getIndexClient(User user) {
        return new RestApiIndexClient(user);
    }

}
