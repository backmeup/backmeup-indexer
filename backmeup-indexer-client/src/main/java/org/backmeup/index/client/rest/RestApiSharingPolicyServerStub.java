package org.backmeup.index.client.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
    private final RestUrlsSharingPolicy urls;

    public RestApiSharingPolicyServerStub(RestApiConfig config) {
        this.urls = new RestUrlsSharingPolicy(config);
    }

    @Override
    public Set<SharingPolicyEntry> getAllOwned(User owner) {
        try {
            URI url = this.urls.forGetAllOwned(owner);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfSharingPolicyEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public Set<SharingPolicyEntry> getAllIncoming(User currUser) {
        try {
            URI url = this.urls.forGetAllIncoming(currUser);
            String body = this.http.get(url, 200);
            return Json.deserializeSetOfSharingPolicyEntries(body);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public SharingPolicyEntry add(User currUser, User sharingWith, SharingPolicyTypeEntry policy,
            String sharedElementID, String name, String description) {
        try {
            URI url = this.urls.forAdd(currUser, sharingWith, policy, sharedElementID, name, description);
            String body = this.http.post(url, "", 200);
            return Json.deserialize(body, SharingPolicyEntry.class);

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String removeOwned(User owner, Long policyID) {
        try {
            URI url = this.urls.forRemove(owner, policyID);
            String body = this.http.delete(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    @Override
    public String removeAllOwned(User owner) {
        try {
            URI url = this.urls.forRemoveAllOwned(owner);
            String body = this.http.delete(url, 200);
            return body;

        } catch (IOException | URISyntaxException e) {
            throw failedToContactServer(e);
        }
    }

    private IndexClientException failedToContactServer(Exception problem) {
        return new IndexClientException("faled to contact sharing policy management server", problem);
    }

}
