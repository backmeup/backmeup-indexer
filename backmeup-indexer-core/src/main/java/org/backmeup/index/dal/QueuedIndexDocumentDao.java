package org.backmeup.index.dal;

import java.util.List;

import org.backmeup.index.core.model.QueuedIndexDocument;

/**
 * The QueuedIndexDocumentDao contains all database relevant operations for the model class IndexDocumentDropOffQueue.
 * It is used to fetch and retrieve persistet information on IndexDocuments which have been dropped of to the queue
 * awaiting import into the user's elastic search instance regarding an running index configuration
 */
public interface QueuedIndexDocumentDao extends BaseDao<QueuedIndexDocument> {

    /**
     * Returns a List of all queued IndexDocuments from the database
     * 
     * @return
     */
    List<QueuedIndexDocument> getAllQueuedIndexDocuments();

    QueuedIndexDocument findQueuedIndexDocumentByEntityId(Long entityId);

    void deleteAll();

}