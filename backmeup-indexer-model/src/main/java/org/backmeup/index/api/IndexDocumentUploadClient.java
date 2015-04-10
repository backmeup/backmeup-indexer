package org.backmeup.index.api;

import java.io.Closeable;
import java.io.IOException;

import org.backmeup.index.model.IndexDocument;

/**
 * A REST API client to the index document upload component.
 * 
 */
public interface IndexDocumentUploadClient extends Closeable {

    /**
     * Takes a given document as created by the indexing plugin uploads it to the indexing backend component which then
     * takes care of distribution and indexing according to the defined sharing policies
     * 
     * @param document
     * @throws IOException
     */
    String uploadForSharing(IndexDocument document) throws IOException;

    @Override
    void close();

}