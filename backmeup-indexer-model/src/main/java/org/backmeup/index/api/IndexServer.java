package org.backmeup.index.api;

import java.io.IOException;
import java.util.Date;
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

    SearchResultAccumulator query(User userId, String query, String filterBySource, String filterByType,
            String filterByJob, String username);

    Set<FileItem> filesForJob(User userId, Long jobId);

    FileInfo fileInfoForFile(User userId, String fileId);

    String thumbnailPathForFile(User userId, String fileId);

    String delete(User userId, Long jobId, Date timestamp);

    /**
     * starts the physical indexing process for a given document. i.e. spins up a private Index instance (e.g. ES) and
     * executes the indexing for the given document -> since document sharing: this method should no longer be directly
     * called by the plugins rather use uploadForSharing instead
     * 
     * @param userId
     * @param document
     * @return
     * @throws IOException
     */
    String index(User userId, IndexDocument document) throws IOException;

}