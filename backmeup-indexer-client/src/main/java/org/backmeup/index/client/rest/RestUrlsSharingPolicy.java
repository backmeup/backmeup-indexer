package org.backmeup.index.client.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.http.client.utils.URIBuilder;
import org.backmeup.index.model.User;
import org.backmeup.index.model.sharing.SharingPolicyEntry.SharingPolicyTypeEntry;

/**
 * Create the RESTful URLs to contact the sharing policy component. Class is used to cover both REST endpoints for
 * SharingPolicies and HeritagePolicies
 * 
 */
public class RestUrlsSharingPolicy {

    private final String host;
    private final int port;
    private final String basePath;

    public RestUrlsSharingPolicy(RestApiConfig config) {
        this(config, "/sharing");
    }

    public RestUrlsSharingPolicy(RestApiConfig config, String path) {
        this.host = config.host;
        this.port = config.port;
        this.basePath = config.basepath + path;
    }

    public URI forGetAllOwned(User owner) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(owner, "owned");
        return urlBuilder.build();
    }

    public URI forGetAllIncoming(User currUser) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(currUser, "incoming");
        return urlBuilder.build();
    }

    public URI forAdd(User fromUser, User withUser, SharingPolicyTypeEntry policyType, String policyValue, String name,
            String description, Date lifespanstart, Date lifespanend) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(fromUser, "");
        addMandatoryParameter(urlBuilder, "withUserId", withUser);
        addMandatoryParameter(urlBuilder, "policyType", policyType);
        if ((policyType == SharingPolicyTypeEntry.Backup) || (policyType == SharingPolicyTypeEntry.Document)) {
            addMandatoryParameter(urlBuilder, "policyValue", policyValue);
        } else if (policyType == SharingPolicyTypeEntry.DocumentGroup) {
            addMandatoryParameterListFromString(urlBuilder, "policyValue", policyValue);
        } else {
            addOptionalParameter(urlBuilder, "policyValue", policyValue);
        }
        addOptionalParameter(urlBuilder, "name", name);
        addOptionalParameter(urlBuilder, "description", description);
        addOptionalParameter(urlBuilder, "lifespanstart", lifespanstart);
        addOptionalParameter(urlBuilder, "lifespanend", lifespanend);
        return urlBuilder.build();
    }

    public URI forUpdate(User currUser, Long policyID, String name, String description, Date lifespanstart,
            Date lifespanend) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(currUser, "update");
        addMandatoryParameter(urlBuilder, "fromUserId", currUser);
        addMandatoryParameter(urlBuilder, "policyID", policyID);
        addOptionalParameter(urlBuilder, "name", name);
        addOptionalParameter(urlBuilder, "description", description);
        addOptionalParameter(urlBuilder, "lifespanstart", lifespanstart);
        addOptionalParameter(urlBuilder, "lifespanend", lifespanend);
        return urlBuilder.build();
    }

    public URI forRemove(User owner, Long policyID) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(owner, "");
        addMandatoryParameter(urlBuilder, "policyID", policyID);
        return urlBuilder.build();
    }

    /**
     * endpoint available only for sharing policies only - not for heritage policies
     */
    public URI forRemoveAllOwned(User owner) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(owner, "all");
        return urlBuilder.build();
    }

    /**
     * endpoint available only for sharing policies only - not for heritage policies
     */
    public URI forAcceptIncomingSharing(User user, Long policyID) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "acceptIncoming");
        addMandatoryParameter(urlBuilder, "policyID", policyID);
        return urlBuilder.build();
    }

    /**
     * endpoint available only for sharing policies only - not for heritage policies
     */
    public URI forDeclineIncomingSharing(User user, Long policyID) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "declineIncoming");
        addMandatoryParameter(urlBuilder, "policyID", policyID);
        return urlBuilder.build();
    }

    /**
     * endpoint available only for heritage policies only - not for sharing policies
     */
    public URI forActivateDeadMannSwitchAndImport(User user) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(user, "deadmannswitch/activate");
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

    private void addMandatoryParameterListFromString(URIBuilder url, String name, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("parameter " + name + " is mandatory");
        }
        try {
            String[] sArr = value.substring(1, value.length() - 1).split(",\\s*");
            List<String> lArr = Arrays.asList(sArr);
            if (lArr.size() <= 1) {
                badRequestMalformedListOfUUIDsParameter(name);
            }
            //test sample on UUIDs
            for (int i = 0; i < lArr.size(); i++) {
                UUID.fromString(lArr.get(i));
            }
        } catch (Exception e) {
            badRequestMalformedListOfUUIDsParameter(name);
        }
        url.addParameter(name, value);
    }

    private void badRequestMalformedListOfUUIDsParameter(String name) {
        List<UUID> l = new ArrayList<UUID>();
        l.add(UUID.randomUUID());
        l.add(UUID.randomUUID());
        throw new IllegalArgumentException(name + " parameter is malformed. Expecting list in syntax: " + l.toString());
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

    private void addOptionalParameter(URIBuilder url, String key, Date value) {
        if (value != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("EE MMM dd hh:mm:ss z yyyy");
            String fDate = formatter.format(value);
            url.addParameter(key, fDate);
        }
    }

}
