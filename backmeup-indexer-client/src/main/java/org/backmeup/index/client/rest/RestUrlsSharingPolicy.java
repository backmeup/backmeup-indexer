package org.backmeup.index.client.rest;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;

/**
 * Create the RESTful URLs to contact the sharing policy component.
 * 
 */
public class RestUrlsSharingPolicy {

    private final String host;
    private final int port;
    private final String basePath;

    public RestUrlsSharingPolicy(RestApiConfig config) {
        this.host = config.host;
        this.port = config.port;
        this.basePath = config.basepath + "/sharing";
    }

    public URI forGetAllOwned(User owner) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(owner, "owned");
        return urlBuilder.build();
    }

    public URI forGetAllIncoming(User currUser) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(currUser, "incoming");
        return urlBuilder.build();
    }

    public URI forAdd(User fromUser, User withUser, SharingPolicyTypeEntry policyType, String policyValue)
            throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(fromUser, "");
        addMandatoryParameter(urlBuilder, "withUserId", withUser);
        addMandatoryParameter(urlBuilder, "policyType", policyType);
        if ((policyType == SharingPolicyTypeEntry.Backup) || (policyType == SharingPolicyTypeEntry.Document)) {
            addMandatoryParameter(urlBuilder, "policyValue", policyValue);
        } else {
            addOptionalParameter(urlBuilder, "policyValue", policyValue);
        }
        return urlBuilder.build();
    }

    public URI forRemove(User owner, Long policyID) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(owner, "");
        addMandatoryParameter(urlBuilder, "policyID", policyID);
        return urlBuilder.build();
    }

    public URI forRemoveAllOwned(User owner) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(owner, "all");
        return urlBuilder.build();
    }

    // private

    private URIBuilder startWithBaseUrl(User userId, String path) throws URISyntaxException {
        return new URIBuilder("http://" + this.host + ":" + this.port + this.basePath + "/" + userId + "/" + path);
    }

    private void addMandatoryParameter(URIBuilder url, String key, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value);
    }

    private void addMandatoryParameter(URIBuilder url, String key, Long value) {
        if (value == null || value == 0) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value.toString());
    }

    private void addMandatoryParameter(URIBuilder url, String key, User value) {
        if (value == null || value.id() == 0) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value.toString());
    }

    private void addMandatoryParameter(URIBuilder url, String key, SharingPolicyTypeEntry value) {
        if (value == null || value.name().isEmpty()) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value.toString());
    }

    private void addOptionalParameter(URIBuilder url, String key, String value) {
        if (value != null && !value.isEmpty()) {
            url.addParameter(key, value);
        }
    }

    private void addOptionalParameter(URIBuilder url, String key, Long value) {
        if (value != null && value != 0) {
            url.addParameter(key, String.valueOf(value));
        }
    }

}
