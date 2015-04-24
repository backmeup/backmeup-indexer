package org.backmeup.index.client.rest;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexServer;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;

/**
 * Adapts the local index client to the remote index server.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class RestApiIndexClient implements IndexClient {

    private final IndexServer server = new RestApiIndexServerStub(RestApiConfig.DEFAULT);
    private final User userId;

    public RestApiIndexClient(User userId) {
        this.userId = userId;
    }

    @Override
    public SearchResultAccumulator queryBackup(String query, String filterBySource, String filterByType,
            String filterByJob, String filterByOwner, String username) {
        return this.server
                .query(this.userId, query, filterBySource, filterByType, filterByJob, filterByOwner, username);
    }

    @Override
    public Set<FileItem> searchAllFileItemsForJob(Long jobId) {
        return this.server.filesForJob(this.userId, jobId);
    }

    @Override
    public FileInfo getFileInfoForFile(String fileId) {
        return this.server.fileInfoForFile(this.userId, fileId);
    }

    @Override
    public String getThumbnailPathForFile(String fileId) {
        return this.server.thumbnailPathForFile(this.userId, fileId);
    }

    @Override
    public void deleteRecordsForUser() {
        this.server.delete(this.userId, null, null);
    }

    @Override
    public void deleteRecordsForUserAndJobAndTimestamp(Long jobId, Date timestamp) {
        this.server.delete(this.userId, jobId, timestamp);
    }

    @Override
    public void deleteRecordsForUserAndDocumentUUID(UUID documentUUID) {
        // only required for the ES Client implementation not the REST API
    }

    @Override
    public void index(IndexDocument document) throws IOException {
        this.server.index(this.userId, document);
    }

    @Override
    public void close() {
    }

}
