package org.backmeup.index.sharing.execution;

/**
 * Takes an IndexDocument produced by the IndexingPlugin and takes care of assigning an ID, checking sharing policies
 * and distributing the document into the proper user space. Takes care of update and delete requests of IndexDocuments
 * within the ES Instance
 *
 */
public class IndexDocumentImportManager {

    public void inportAllPendingDocumentsToES(Long userID) {

    }

}