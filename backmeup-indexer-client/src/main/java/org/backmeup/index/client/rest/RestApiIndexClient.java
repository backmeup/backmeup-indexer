package org.backmeup.index.client.rest;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexServer;
import org.backmeup.index.client.config.Configuration;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects the local index client to the remote index server.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class RestApiIndexClient implements IndexClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final IndexServer server;
    private final User user;

    public RestApiIndexClient(User currUser) {
        this.user = currUser;
        this.server = new RestApiIndexServerStub(getRESTServerEndpointLocation());
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
    public SearchResultAccumulator queryBackup(String query, String filterBySource, String filterByType, String filterByJob,
            String filterByOwner, String filterByTag, String username, Long queryOffSetStart, Long queryMaxResults) {
        return this.server.query(this.user, query, filterBySource, filterByType, filterByJob, filterByOwner, filterByTag, username,
                queryOffSetStart, queryMaxResults);
    }

    @Override
    public Set<FileItem> searchAllFileItemsForJob(Long jobId) {
        return this.server.filesForJob(this.user, jobId);
    }

    @Override
    public FileInfo getFileInfoForFile(String fileId) {
        return this.server.fileInfoForFile(this.user, fileId);
    }

    @Override
    public String getThumbnailPathForFile(String fileId) {
        return this.server.thumbnailPathForFile(this.user, fileId);
    }

    @Override
    public void deleteRecordsForUser() {
        this.server.delete(this.user, null, null);
    }

    @Override
    public void deleteRecordsForUserAndJobAndTimestamp(Long jobId, Date timestamp) {
        this.server.delete(this.user, jobId, timestamp);
    }

    @Override
    public void deleteRecordsForUserAndDocumentUUID(UUID documentUUID) {
        // only required for the ES Client implementation not the REST API
    }

    @Override
    public void index(IndexDocument document) throws IOException {
        this.server.index(this.user, document);
    }

    @Override
    public void close() {
    }

}
