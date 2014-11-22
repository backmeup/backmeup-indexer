package org.backmeup.index.client.rest;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;

public class RestUrls {

    // TODO PK read values from config
    private String host = "127.0.0.1";
    private int port = 7654;
    private String basePath = "/index";

    public URI forQuery(Long userId, String query, String filterBySource, String filterByType, String filterByJob, String username)
            throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(userId, "");
        addMandatoryParameter(urlBuilder, "query", query);
        addOptionalParameter(urlBuilder, "source", filterBySource);
        addOptionalParameter(urlBuilder, "type", filterByType);
        addOptionalParameter(urlBuilder, "job", filterByJob);
        addMandatoryParameter(urlBuilder, "username", username);
        return urlBuilder.build();
    }

    public URI forFilesOfJob(Long userId, Long jobId) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(userId, "files");
        addMandatoryParameter(urlBuilder, "job", jobId);
        return urlBuilder.build();
    }

    public URI forFileInfo(Long userId, String fileId) throws URISyntaxException {
        return startWithBaseUrl(userId, "/files/" + fileId + "/info").build();
    }

    public URI forThumbnail(Long userId, String fileId) throws URISyntaxException {
        return startWithBaseUrl(userId, "/files/" + fileId + "/thumbnail").build();
    }

    public URI forDelete(Long userId, Long jobId, Long timestamp) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(userId, "");
        addOptionalParameter(urlBuilder, "job", jobId);
        addOptionalParameter(urlBuilder, "time", timestamp);
        return urlBuilder.build();
    }

    public URI forNewDocument(Long userId) throws URISyntaxException {
        return startWithBaseUrl(userId, "").build();
    }

    // private

    private URIBuilder startWithBaseUrl(Long userId, String path) throws URISyntaxException {
        return new URIBuilder("http://" + host + ":" + port + basePath + "/" + userId + "/" + path);
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
