package org.backmeup.index.api;

import java.io.IOException;
import java.util.Set;

import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;

/**
 * Artificial interface to keep the client and the server of REST API in sync.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public interface IndexServer {

    SearchResultAccumulator query(User userId, String query, String filterBySource, String filterByType, String filterByJob, String username);

    Set<FileItem> filesForJob(User userId, Long jobId);

    FileInfo fileInfoForFile(User userId, String fileId);

    String thumbnailPathForFile(User userId, String fileId);

    String delete(User userId, Long jobId, Long timestamp);

    String index(User userId, IndexDocument document) throws IOException;

}