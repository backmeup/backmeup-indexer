package org.backmeup.index.client.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import org.apache.http.client.utils.URIBuilder;
import org.backmeup.index.model.User;

/**
 * Create the RESTful URLs to contact the index component.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class RestUrlsIndex {

    private final String host;
    private final int port;
    private final String basePath;

    public RestUrlsIndex(RestApiConfig config) {
        this.host = config.host;
        this.port = config.port;
        this.basePath = config.basepath + "/index";
    }

    public URI forQuery(User userId, String query, String filterBySource, String filterByType, String filterByJob,
            String filterByOwner, String filterByTag, String username, Long queryOffSetStart, Long queryMaxResults)
            throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(userId, "");
        addMandatoryParameter(urlBuilder, "query", query);
        addOptionalParameter(urlBuilder, "source", filterBySource);
        addOptionalParameter(urlBuilder, "type", filterByType);
        addOptionalParameter(urlBuilder, "job", filterByJob);
        addOptionalParameter(urlBuilder, "owner", filterByOwner);
        addOptionalParameter(urlBuilder, "tag", filterByTag);
        addMandatoryParameter(urlBuilder, "username", username);
        addOptionalParameter(urlBuilder, "offset", queryOffSetStart);
        addOptionalParameter(urlBuilder, "maxresults", queryMaxResults);
        return urlBuilder.build();
    }

    public URI forFilesOfJob(User userId, Long jobId) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(userId, "files");
        addMandatoryParameter(urlBuilder, "job", jobId);
        return urlBuilder.build();
    }

    public URI forFileInfo(User userId, String fileId) throws URISyntaxException {
        return startWithBaseUrl(userId, "/files/" + fileId + "/info").build();
    }

    public URI forThumbnail(User userId, String fileId) throws URISyntaxException {
        return startWithBaseUrl(userId, "/files/" + fileId + "/thumbnail").build();
    }

    public URI forDelete(User userId, Long jobId, Date timestamp) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(userId, "");
        addOptionalParameter(urlBuilder, "job", jobId);
        addOptionalParameter(urlBuilder, "time", timestamp.getTime());
        return urlBuilder.build();
    }

    public URI forDelete(User userId, UUID indexFragmentUUID) throws URISyntaxException {
        URIBuilder urlBuilder = startWithBaseUrl(userId, "");
        addMandatoryParameter(urlBuilder, "document", indexFragmentUUID);
        return urlBuilder.build();
    }

    public URI forNewDocument(User userId) throws URISyntaxException {
        return startWithBaseUrl(userId, "").build();
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

    private void addMandatoryParameter(URIBuilder url, String key, UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("parameter " + key + " is mandatory");
        }
        url.addParameter(key, value.toString());
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
