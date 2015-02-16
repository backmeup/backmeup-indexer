package org.backmeup.index.sharing.execution;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.serializer.Json;
import org.junit.Before;
import org.junit.Test;

public class IndexDocumentDropOffTest {

    @Before
    public void before() {

    }

    @Test
    public void testDropOffDocumentQueueFirstInFirstOut() {
        IndexDocumentDropOffQueue queue = IndexDocumentDropOffQueue.getInstance();
        queue.addIndexDocument(createIndexDocument(1L));
        assertEquals(1, queue.size());
        queue.addIndexDocument(createIndexDocument(2L));
        queue.addIndexDocument(createIndexDocument(3L));
        assertEquals(3, queue.size());
        IndexDocument doc = queue.getNext();
        assertEquals(doc.getFields().get(IndexFields.FIELD_OWNER_ID), 1L);
        assertEquals(2, queue.size());
    }

    private IndexDocument createIndexDocument(Long userID) {
        try {
            File fIndexDocument = new File("src/test/resources/sampleIndexDocument.serindexdocument");
            String sampleFragment = FileUtils.readFileToString(fIndexDocument, "UTF-8");
            IndexDocument doc = Json.deserialize(sampleFragment, IndexDocument.class);
            doc.field(IndexFields.FIELD_OWNER_ID, userID);
            return doc;
        } catch (IOException e) {
            return new IndexDocument();
        }
    }
}
