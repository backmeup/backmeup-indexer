package org.backmeup.index.client.rest;

import java.util.Set;

import org.backmeup.index.api.SharingPolicyClient;
import org.backmeup.index.api.SharingPolicyServer;
import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;

/**
 * Adapts the local sharing policy client to the remote sharing policy server.
 * 
 */
public class RestApiSharingPolicyClient implements SharingPolicyClient {

    private final SharingPolicyServer server = new RestApiSharingPolicyServerStub(RestApiConfig.DEFAULT);
    private final User currUser;

    public RestApiSharingPolicyClient(User user) {
        this.currUser = user;
    }

    @Override
    public Set<SharingPolicyEntry> getAllOwned() {
        return this.server.getAllOwned(this.currUser);
    }

    @Override
    public Set<SharingPolicyEntry> getAllIncoming() {
        return this.server.getAllIncoming(this.currUser);
    }

    @Override
    public SharingPolicyEntry add(User sharingWith, SharingPolicyTypeEntry policy, String sharedElementID) {
        return this.server.add(this.currUser, sharingWith, policy, sharedElementID);
    }

    @Override
    public void remove(Long policyID) {
        this.server.removeOwned(this.currUser, policyID);
    }

    @Override
    public void removeAllOwned() {
        this.server.removeAllOwned(this.currUser);
    }

    @Override
    public void close() {
    }

}
