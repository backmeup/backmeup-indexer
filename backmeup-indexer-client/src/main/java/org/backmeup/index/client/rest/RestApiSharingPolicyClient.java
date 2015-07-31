package org.backmeup.index.client.rest;

import java.util.Date;
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
    public SharingPolicyEntry add(User sharingWith, SharingPolicyTypeEntry policy, String sharedElementID, String name,
            String description, Date lifespanstart, Date lifespanend) {
        return this.server.add(this.currUser, sharingWith, policy, sharedElementID, name, description, lifespanstart,
                lifespanend);
    }

    @Override
    public SharingPolicyEntry update(Long policyID, String name, String description, Date lifespanstart,
            Date lifespanend) {
        return this.server.update(this.currUser, policyID, name, description, lifespanstart, lifespanend);
    }

    @Override
    public String removeOwned(Long policyID) {
        return this.server.removeOwned(this.currUser, policyID);
    }

    @Override
    public String removeAllOwned() {
        return this.server.removeAllOwned(this.currUser);
    }

    @Override
    public String acceptIncomingSharing(Long policyID) {
        return this.server.acceptIncomingSharing(this.currUser, policyID);
    }

    @Override
    public String declineIncomingSharing(Long policyID) {
        return this.server.declineIncomingSharing(this.currUser, policyID);
    }

    @Override
    public void close() {
    }

}
