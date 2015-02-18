package org.backmeup.index.sharing.execution;

import java.util.LinkedList;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.dal.QueuedIndexDocumentDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.utils.cdi.RunRequestScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Takes IndexDocuments from IndexingPlugin, adds it to the non persistent queue
 *
 */
@ApplicationScoped
public class IndexDocumentDropOffQueue {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private QueuedIndexDocumentDao dao;

    //a list containing all queued IndexDocuments (synced with db) - waiting for import 
    private LinkedList<QueuedIndexDocument> sortedQueue = new LinkedList<QueuedIndexDocument>();

    @RunRequestScoped
    public void startupDroOffQueue() {
        // Initialization drop off queue and sync of pending records from DB
        syncQueueAfterStartupFromDBRecords();
        this.log.debug("startup() IndexDocumentDropOffQueue (ApplicationScoped) completed");
    }

    @RunRequestScoped
    public void shutdownDroOffQueue() {
        this.log.debug("shutdown IndexDocumentDropOffQueue (ApplicationScoped) completed");
    }

    // ========================================================================

    /*private static IndexDocumentDropOffQueue instance;

    public static IndexDocumentDropOffQueue getInstance() {
        if (instance == null) {
            synchronized (IndexDocumentDropOffQueue.class) {
                instance = new IndexDocumentDropOffQueue();
            }
        }
        return instance;
    }*/

    public IndexDocumentDropOffQueue() {
        //
    }

    public synchronized void addIndexDocument(IndexDocument indexDoc) {
        QueuedIndexDocument qidoc = this.dao.save(new QueuedIndexDocument(indexDoc));
        this.sortedQueue.add(qidoc);
    }

    public synchronized IndexDocument getNext() {
        QueuedIndexDocument next = this.sortedQueue.poll();
        if (next != null) {
            QueuedIndexDocument dbIndexDoc = this.dao.findQueuedIndexDocumentByEntityId(next.getId());
            this.dao.delete(dbIndexDoc);
            return next.getIndexDocument();
        }
        return null;
    }

    public synchronized int size() {
        return this.sortedQueue.size();
    }

    /**
     * When initializing the queue load entries from database
     */
    private synchronized void syncQueueAfterStartupFromDBRecords() {

        this.log.debug("syncQueueAfterStartupFromDBRecords");
        // get all queued instances according to the DB entries
        this.sortedQueue = new LinkedList<QueuedIndexDocument>(this.dao.getAllQueuedIndexDocuments());
        this.log.debug("found " + this.sortedQueue.size() + " queued index document records from DB");
    }

    /**
     * A protected method, just for JUnit Testing the queue
     */
    protected void syncQueueAfterStartupFromDBRecords4JUnitTests() {
        this.syncQueueAfterStartupFromDBRecords();
    }

}
