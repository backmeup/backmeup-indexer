package org.backmeup.index.sharing;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.QueuedIndexDocument;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.serializer.Json;

public class IndexDocumentTestingUtils {

    public static IndexDocument createIndexDocument(Long userID) {
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

    public QueuedIndexDocument createQueuedIndexDocument(Long userID) {
        IndexDocument indexDoc = createIndexDocument(userID);
        return new QueuedIndexDocument(indexDoc);
    }

    public QueuedIndexDocument createQueuedIndexDocument(IndexDocument indexDoc) {
        if (!indexDoc.getFields().containsKey(IndexFields.FIELD_INDEX_DOCUMENT_UUID)) {
            indexDoc.field(IndexFields.FIELD_INDEX_DOCUMENT_UUID, UUID.randomUUID().toString());
        }
        return new QueuedIndexDocument(indexDoc);
    }

}
