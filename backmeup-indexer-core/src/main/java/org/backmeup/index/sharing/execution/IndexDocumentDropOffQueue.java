package org.backmeup.index.sharing.execution;

import java.util.LinkedList;

import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.model.IndexDocument;

/**
 * Takes IndexDocuments from IndexingPlugin, adds it to the non persistent queue
 *
 */
public class IndexDocumentDropOffQueue {

    private LinkedList<QueuedIndexDocument> sortedQueue = new LinkedList<QueuedIndexDocument>();

    private static IndexDocumentDropOffQueue instance;

    public static IndexDocumentDropOffQueue getInstance() {
        if (instance == null) {
            synchronized (IndexDocumentDropOffQueue.class) {
                instance = new IndexDocumentDropOffQueue();
            }
        }
        return instance;
    }

    private IndexDocumentDropOffQueue() {
        //
    }

    public void addIndexDocument(IndexDocument indexDoc) {
        this.sortedQueue.add(new QueuedIndexDocument(indexDoc));
    }

    public IndexDocument getNext() {
        return this.sortedQueue.poll().getIndexDocument();
    }

    public int size() {
        return this.sortedQueue.size();
    }

}
