package org.backmeup.index.api;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

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

    SearchResultAccumulator query(User user, String query, String filterBySource, String filterByType, String filterByJob,
            String filterByOwner, String filterByTag, String username, Long queryOffSetStart, Long queryMaxResults);

    Set<FileItem> filesForJob(User user, Long jobId);

    FileInfo fileInfoForFile(User user, String fileId);

    String thumbnailPathForFile(User user, String fileId);

    String delete(User userId, Long job, Date timestamp);

    String delete(User user, UUID indexFragmentUUID);

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
    String index(User user, IndexDocument document) throws IOException;

}