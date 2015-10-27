package org.backmeup.index.client.rest;

import java.util.Date;
import java.util.Set;

import org.backmeup.index.api.SharingPolicyClient;
import org.backmeup.index.api.SharingPolicyServer;
import org.backmeup.index.client.config.Configuration;
import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts the local sharing policy client to the remote sharing policy server.
 * 
 */
public class RestApiSharingPolicyClient implements SharingPolicyClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SharingPolicyServer server;
    private final User currUser;

    public RestApiSharingPolicyClient(User user) {
        this.currUser = user;
        this.server = new RestApiSharingPolicyServerStub(getRESTServerEndpointLocation());
    }

    private RestApiConfig getRESTServerEndpointLocation() {
        RestApiConfig config;
        try {
            String host = Configuration.getProperty("backmeup.indexer.rest.host");
            String port = Configuration.getProperty("backmeup.indexer.rest.port");
            String baseurl = Configuration.getProperty("backmeup.indexer.rest.baseurl");
            //check if a configuration was provided or if we're using the default config
            if ((host != null) && (port != null) && (baseurl != null)) {
                config = new RestApiConfig(host, Integer.valueOf(port), baseurl);
            } else {
                config = RestApiConfig.DEFAULT;
            }
        } catch (Exception e) {
            this.logger
                    .info("not able to read host, port or baseurl from backmeup-index-client.properties for index-client REST endpoint location, defaulting to static configuration");
            config = RestApiConfig.DEFAULT;
        }
        return config;
    }

    @Override
    public Set<SharingPolicyEntry> getAllOwnedSharingPolicies() {
        return this.server.getAllOwnedSharingPolicies(this.currUser);
    }

    @Override
    public Set<SharingPolicyEntry> getAllIncomingSharingPolicies() {
        return this.server.getAllIncomingSharingPolicies(this.currUser);
    }

    @Override
    public SharingPolicyEntry addSharingPolicy(User sharingWith, SharingPolicyTypeEntry policy, String sharedElementID, String name,
            String description, Date lifespanstart, Date lifespanend) {
        return this.server.addSharingPolicy(this.currUser, sharingWith, policy, sharedElementID, name, description, lifespanstart,
                lifespanend);
    }

    @Override
    public SharingPolicyEntry updateSharingPolicy(Long policyID, String name, String description, Date lifespanstart, Date lifespanend) {
        return this.server.updateSharingPolicy(this.currUser, policyID, name, description, lifespanstart, lifespanend);
    }

    @Override
    public String removeOwnedSharingPolicy(Long policyID) {
        return this.server.removeOwnedSharingPolicy(this.currUser, policyID);
    }

    @Override
    public String removeAllOwnedSharingPolicies() {
        return this.server.removeAllOwnedSharingPolicies(this.currUser);
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

    //-------------------- Heritage related operations -----------------------//

    @Override
    public Set<SharingPolicyEntry> getAllOwnedHeritagePolicies() {
        return this.server.getAllOwnedHeritagePolicies(this.currUser);
    }

    @Override
    public Set<SharingPolicyEntry> getAllIncomingHeritagePolicies() {
        return this.server.getAllIncomingHeritagePolicies(this.currUser);
    }

    @Override
    public SharingPolicyEntry addHeritagePolicy(User sharingWith, SharingPolicyTypeEntry policy, String sharedElementID, String name,
            String description, Date lifespanstart, Date lifespanend) {
        return this.server.addHeritagePolicy(this.currUser, sharingWith, policy, sharedElementID, name, description, lifespanstart,
                lifespanend);
    }

    @Override
    public SharingPolicyEntry updateHeritagePolicy(Long policyID, String name, String description, Date lifespanstart, Date lifespanend) {
        return this.server.updateHeritagePolicy(this.currUser, policyID, name, description, lifespanstart, lifespanend);

    }

    @Override
    public String removeOwnedHeritagePolicy(Long policyID) {
        return this.server.removeOwnedHeritagePolicy(this.currUser, policyID);
    }

    @Override
    public String activateDeadMannSwitchAndImport() {
        return this.server.activateDeadMannSwitchAndImport(this.currUser);
    }

}
