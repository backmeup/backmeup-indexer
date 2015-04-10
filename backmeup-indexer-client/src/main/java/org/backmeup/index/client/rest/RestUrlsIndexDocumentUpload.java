package org.backmeup.index.client.rest;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.backmeup.index.model.User;

/**
 * Create the RESTful URLs to contact the index document upload component.
 * 
 */
public class RestUrlsIndexDocumentUpload {

    private final String host;
    private final int port;
    private final String basePath;

    public RestUrlsIndexDocumentUpload(RestApiConfig config) {
        this.host = config.host;
        this.port = config.port;
        this.basePath = config.basepath + "/upload";
    }

    public URI forUploadForSharing(User userId) throws URISyntaxException {
        return startWithBaseUrl(userId, "").build();
    }

    // private

    private URIBuilder startWithBaseUrl(User userId, String path) throws URISyntaxException {
        return new URIBuilder("http://" + this.host + ":" + this.port + this.basePath + "/" + userId + "/" + path);
    }

}
