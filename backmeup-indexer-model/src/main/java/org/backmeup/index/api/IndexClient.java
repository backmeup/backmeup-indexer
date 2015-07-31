package org.backmeup.index.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;

/**
 * A client to the index. The client can be directly using Elastic Search or call the Rest API.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
public interface IndexClient extends Closeable {


    /**
     * Querying the backuped and indexed data of a given user by search queries, filters, etc.
     * @param query the query string including wildcards etc
     * @param filterBySource by plugin source
     * @param filterByType by data type e.g. image, html, etc.
     * @param filterByJob by a specific backupjob
     * @param owner query for a specific userID
     * @param username query by username e.g. to distinguish owner and sharing partner
     * @param tag provide tags to restrict the query
     * @param offSetStart when offSetStart is set to 100 then the first 1-99 results will not be returned
     * @param maxResults limits the number of returned results e.g. to 50 search results
     * @return
     */
    SearchResultAccumulator queryBackup(String query, String filterBySource, String filterByType, String filterByJob,
            String owner, String username, String tag, Long offSetStart, Long maxResults);

    Set<FileItem> searchAllFileItemsForJob(Long jobId);

    FileInfo getFileInfoForFile(String fileId);

    String getThumbnailPathForFile(String fileId);

    void deleteRecordsForUser();

    /**
     * Removes a given index fragment via its document UUID from the user's index
     * 
     * @param documentUUID
     */
    public void deleteRecordsForUserAndDocumentUUID(UUID documentUUID);

    void deleteRecordsForUserAndJobAndTimestamp(Long jobId, Date timestamp);

    /**
     * starts the physical indexing process for a given document. i.e. spins up a private Index instance (e.g. ES) and
     * executes the indexing for the given document -> since document sharing: this method should no longer be directly
     * called by the plugins rather use uploadForSharing instead
     * 
     * @param document
     * @throws IOException
     */
    void index(IndexDocument document) throws IOException;

    @Override
    void close();

}