package org.backmeup.index.api;

import java.io.IOException;

import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;

/**
 * Artificial interface to keep the client and the server of REST API in sync.
 * 
 */
public interface IndexDocumentUploadServer {

    /**
     * Takes a given document as created by the indexing plugin uploads it to the indexing backend component which then
     * takes care of distribution and indexing according to the defined sharing policies
     * 
     * @param document
     * @throws IOException
     */
    String uploadForSharing(User currUser, IndexDocument document) throws IOException;

}