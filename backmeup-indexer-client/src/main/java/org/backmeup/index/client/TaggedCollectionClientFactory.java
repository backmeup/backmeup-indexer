package org.backmeup.index.client;

import org.backmeup.index.api.TaggedCollectionClient;
import org.backmeup.index.client.rest.RestApiTaggedCollectionClient;
import org.backmeup.index.model.User;

public class TaggedCollectionClientFactory {

    public TaggedCollectionClient getTaggedCollectionClient(Long userId) {
        return getTaggedCollectionClient(new User(userId));
    }

    public TaggedCollectionClient getTaggedCollectionClient(User user) {
        return new RestApiTaggedCollectionClient(user);
    }

}
