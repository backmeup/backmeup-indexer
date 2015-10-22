package org.backmeup.index.client.rest;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

/**
 * Create the RESTful URLs to contact the index document upload component.
 * 
 */
public class RestUrlsUserMappingHelper {

    private final String host;
    private final int port;
    private final String basePath;

    public RestUrlsUserMappingHelper(RestApiConfig config) {
        this.host = config.host;
        this.port = config.port;
        this.basePath = config.basepath + "/user/mapping";
    }

    public URI forCreateUserMapping(Long bmuUserId, String keyserverUserId) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl("add");
        addMandatoryParameter(urlBuilder, "bmuuserid", bmuUserId + "");
        addMandatoryParameter(urlBuilder, "keyserveruserid", keyserverUserId + "");
        return urlBuilder.build();
    }

    // private
    private URIBuilder startWithBaseUrl(String path) throws URISyntaxException {
        return new URIBuilder("http://" + this.host + ":" + this.port + this.basePath + "/" + path);
    }

    private void addMandatoryParameter(URIBuilder url, String key, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value);
    }

}
