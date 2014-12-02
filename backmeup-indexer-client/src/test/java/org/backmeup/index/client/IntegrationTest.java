package org.backmeup.index.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.serializer.JsonSerializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("only integration with running and deployed Indexer WAR")
public class IntegrationTest {

    private static final long USER = 16384;

    private IndexClient client;

    @Before
    public void connectToIndex() {
        client = new IndexClientFactory().getIndexClient(USER);
    }

    @After
    public void closeIndex() {
        client.close();
    }

    @Test
    public void shouldIndexAndQueryAndDelete() throws IOException {
        IndexDocument document = deserializeStoredDocument();
        assertEquals(USER, document.getFields().get(IndexFields.FIELD_OWNER_ID));
        client.index(document);

        assertHasDocuments();

        deleteDocument(document);

        assertHasNoDocuments();
    }

    private IndexDocument deserializeStoredDocument() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("indexDocument.json")) {
            String json = IOUtils.toString(resource);
            return JsonSerializer.deserialize(json, IndexDocument.class);
        }
    }

    private void assertHasDocuments() {
        SearchResultAccumulator result = client.queryBackup("*", null, null, null, "username");
        //TODO PK,AL not the proper asserts here
        assertTrue(result.getFiles().size() > 0);
    }

    private void deleteDocument(IndexDocument document) {
        Long timestamp = Long.valueOf((String) document.getFields().get(IndexFields.FIELD_BACKUP_AT));
        Long jobid = Long.valueOf((String) document.getFields().get(IndexFields.FIELD_JOB_ID));
        client.deleteRecordsForJobAndTimestamp(jobid, timestamp);
    }

    private void assertHasNoDocuments() {
        SearchResultAccumulator result = client.queryBackup("*", null, null, null, "username");
        //TODO PK,AL not the proper asserts here
        assertTrue(result.getFiles().isEmpty());
    }

}
