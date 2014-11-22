package org.backmeup.index.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

/**
 * A client to the index. The client can be directly using Elastic Search or
 * call the Rest API.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public interface IndexClient extends Closeable {

    SearchResultAccumulator queryBackup(String query, String filterBySource, String filterByType, String filterByJob, String username);

    Set<FileItem> searchAllFileItemsForJob(Long jobId);

    FileInfo getFileInfoForFile(String fileId);

    String getThumbnailPathForFile(String fileId);

    void deleteRecordsForUser();

    void deleteRecordsForJobAndTimestamp(Long jobId, Long timestamp);

    void index(IndexDocument document) throws IOException;

    @Override
    void close();

}