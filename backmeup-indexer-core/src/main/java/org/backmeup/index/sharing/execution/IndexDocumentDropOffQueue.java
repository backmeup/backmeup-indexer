package org.backmeup.index.sharing.execution;

import java.util.LinkedList;

import org.backmeup.index.model.IndexDocument;

/**
 * Takes IndexDocuments from IndexingPlugin, adds it to the non persistent queue
 *
 */
public class IndexDocumentDropOffQueue {

    private LinkedList<IndexDocument> sortedQueue = new LinkedList<IndexDocument>();

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
        this.sortedQueue.add(indexDoc);
    }

    public IndexDocument getNext() {
        return this.sortedQueue.poll();
    }

    public int size() {
        return this.sortedQueue.size();
    }

}
