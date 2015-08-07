package org.backmeup.index.client.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Set;

import org.backmeup.index.api.SharingPolicyServer;
import org.backmeup.index.client.IndexClientException;
import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;
import org.backmeup.index.serializer.Json;

/**
 * Remote stub of the RESTful sharing policy component.
 * 
 */
public class RestApiSharingPolicyServerStub implements SharingPolicyServer {

    private final HttpMethods http = new HttpMethods();
    private final RestUrlsSharingPolicy urlsForSharing;
    private final RestUrlsSharingPolicy urlsForHeritage;

    public RestApiSharingPolicyServerStub(RestApiConfig config) {
        this.urlsForSharing = new RestUrlsSharingPolicy(config, "/sharing");
        this.urlsForHeritage = new RestUrlsSharingPolicy(config, "/sharing/heritage");
    }

    @Override
    public Set<SharingPolicyEntry> getAllOwnedSharingPolicies(User owner) {
        try {
            URI url = this.urlsForSharing.forGetAllOwned(owner);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfSharingPolicyEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public Set<SharingPolicyEntry> getAllIncomingSharingPolicies(User currUser) {
        try {
            URI url = this.urlsForSharing.forGetAllIncoming(currUser);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfSharingPolicyEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public SharingPolicyEntry addSharingPolicy(User currUser, User sharingWith, SharingPolicyTypeEntry policy,
            String sharedElementID, String name, String description, Date lifespanstart, Date lifespanend) {
        try {
            URI url = this.urlsForSharing.forAdd(currUser, sharingWith, policy, sharedElementID, name, description,
                    lifespanstart, lifespanend);
            String body = this.http.post(url, "", 200);
            return Json.deserialize(body, SharingPolicyEntry.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public SharingPolicyEntry updateSharingPolicy(User currUser, Long policyID, String name, String description,
            Date lifespanstart, Date lifespanend) {
        try {
            URI url = this.urlsForSharing.forUpdate(currUser, policyID, name, description, lifespanstart, lifespanend);
            String body = this.http.post(url, "", 200);
            return Json.deserialize(body, SharingPolicyEntry.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String removeOwnedSharingPolicy(User owner, Long policyID) {
        try {
            URI url = this.urlsForSharing.forRemove(owner, policyID);
            String body = this.http.delete(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String removeAllOwnedSharingPolicies(User owner) {
        try {
            URI url = this.urlsForSharing.forRemoveAllOwned(owner);
            String body = this.http.delete(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    private IndexClientException failedToContactServer(Exception problem) {
        return new IndexClientException("faled to contact sharing policy management server", problem);
    }

    @Override
    public String acceptIncomingSharing(User user, Long policyID) {
        try {
            URI url = this.urlsForSharing.forAcceptIncomingSharing(user, policyID);
            String body = this.http.post(url, "", 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String declineIncomingSharing(User user, Long policyID) {
        try {
            URI url = this.urlsForSharing.forDeclineIncomingSharing(user, policyID);
            String body = this.http.post(url, "", 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    //-----------------------HERITAGE -----------------------------//

    @Override
    public Set<SharingPolicyEntry> getAllOwnedHeritagePolicies(User owner) {
        try {
            URI url = this.urlsForHeritage.forGetAllOwned(owner);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfSharingPolicyEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public Set<SharingPolicyEntry> getAllIncomingHeritagePolicies(User forUser) {
        try {
            URI url = this.urlsForHeritage.forGetAllIncoming(forUser);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfSharingPolicyEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public SharingPolicyEntry addHeritagePolicy(User currUser, User sharingWith, SharingPolicyTypeEntry policy,
            String sharedElementID, String name, String description, Date lifespanstart, Date lifespanend) {
        try {
            URI url = this.urlsForHeritage.forAdd(currUser, sharingWith, policy, sharedElementID, name, description,
                    lifespanstart, lifespanend);
            String body = this.http.post(url, "", 200);
            return Json.deserialize(body, SharingPolicyEntry.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public SharingPolicyEntry updateHeritagePolicy(User currUser, Long policyID, String name, String description,
            Date lifespanstart, Date lifespanend) {
        try {
            URI url = this.urlsForHeritage.forUpdate(currUser, policyID, name, description, lifespanstart, lifespanend);
            String body = this.http.post(url, "", 200);
            return Json.deserialize(body, SharingPolicyEntry.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String removeOwnedHeritagePolicy(User owner, Long policyID) {
        try {
            URI url = this.urlsForHeritage.forRemove(owner, policyID);
            String body = this.http.delete(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String activateDeadMannSwitchAndImport(User currUser) {
        try {
            URI url = this.urlsForHeritage.forActivateDeadMannSwitchAndImport(currUser);
            String body = this.http.post(url, "", 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

}
