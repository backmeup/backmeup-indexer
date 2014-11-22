package org.backmeup.index.client.rest;

import java.io.IOException;
import java.util.Set;

import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexServer;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;

/**
 * Adapts the local index client to the remove index server.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public class RestApiIndexClient implements IndexClient {

    private final IndexServer server = new RestApiServerStub();
    private final Long userId;

    public RestApiIndexClient(Long userId) {
        this.userId = userId;
    }

    @Override
    public SearchResultAccumulator queryBackup(String query, String filterBySource, String filterByType, String filterByJob, String username) {
        return server.query(userId, query, filterBySource, filterByType, filterByJob, username);
    }

    @Override
    public Set<FileItem> searchAllFileItemsForJob(Long jobId) {
        return server.filesForJob(userId, jobId);
    }

    @Override
    public FileInfo getFileInfoForFile(String fileId) {
        return server.fileInfoForFile(userId, fileId);
    }

    @Override
    public String getThumbnailPathForFile(String fileId) {
        return server.thumbnailPathForFile(userId, fileId);
    }

    @Override
    public void deleteRecordsForUser() {
        server.delete(userId, null, null);
    }

    @Override
    public void deleteRecordsForJobAndTimestamp(Long jobId, Long timestamp) {
        server.delete(timestamp, jobId, timestamp);
    }

    @Override
    public void index(IndexDocument document) throws IOException {
        server.index(userId, document);
    }

    @Override
    public void close() {
    }

}
