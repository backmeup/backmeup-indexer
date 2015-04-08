package org.backmeup.index.client;

import org.backmeup.index.api.SharingPolicyClient;
import org.backmeup.index.client.rest.RestApiSharingPolicyClient;
import org.backmeup.index.model.User;

public class SharingPolicyClientFactory {

    public SharingPolicyClient getSharingPolicyClient(Long userId) {
        return getSharingPolicyClient(new User(userId));
    }

    public SharingPolicyClient getSharingPolicyClient(User user) {
        return new RestApiSharingPolicyClient(user);
    }

}
