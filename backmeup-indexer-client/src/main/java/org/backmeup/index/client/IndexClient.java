package org.backmeup.index.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.SearchResultAccumulator;

public interface IndexClient extends Closeable {

    SearchResultAccumulator queryBackup(String query, Map<String, List<String>> filters, String username);

    Set<FileItem> searchAllFileItemsForJob(Long jobId);

    FileInfo getFileInfoForFile(String fileId);

    String getThumbnailPathForFile(String fileId);

    void deleteRecordsForUser();

    void deleteRecordsForJobAndTimestamp(Long jobId, Long timestamp);

    void index(IndexDocument document) throws IOException;

    @Override
    void close();
}