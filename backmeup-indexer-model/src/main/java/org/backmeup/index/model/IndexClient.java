package org.backmeup.index.model;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;

public interface IndexClient extends Closeable {

    SearchResultAccumulator queryBackup(String query, String source, String type, String job, String username);

    Set<FileItem> searchAllFileItemsForJob(Long jobId);

    FileInfo getFileInfoForFile(String fileId);

    String getThumbnailPathForFile(String fileId);

    void deleteRecordsForUser();

    void deleteRecordsForJobAndTimestamp(Long jobId, Long timestamp);

    void index(IndexDocument document) throws IOException;

    @Override
    void close();

}