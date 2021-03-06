package org.backmeup.index.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class IndexRestClientIntegrationTest {

    private static final User USER = new User(16384L, "testtoken");

    private IndexClient client;

    @Before
    public void connectToIndex() {
        this.client = new IndexClientFactory().getIndexClient(USER);
    }

    @After
    public void closeIndex() {
        this.client.close();
    }

    @Test
    @Ignore("no longer working due to keyserver integration - need to figure out a different testsetup")
    public void shouldIndexAndQueryAndDelete() throws IOException {
        IndexDocument document = deserializeStoredDocument();
        assertEquals(USER.id(), document.getFields().get(IndexFields.FIELD_OWNER_ID));
        this.client.index(document);

        assertHasDocuments();

        deleteDocument(document);

        assertHasNoDocuments();
    }

    private IndexDocument deserializeStoredDocument() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("indexDocument.json")) {
            String json = IOUtils.toString(resource);
            return Json.deserialize(json, IndexDocument.class);
        }
    }

    private void assertHasDocuments() {
        SearchResultAccumulator result = this.client.queryBackup("*", null, null, null, null, null, "username", null, null);
        //TODO PK,AL not the proper asserts here
        assertTrue(result.getFiles().size() > 0);
    }

    private void deleteDocument(IndexDocument document) {
        Long timestamp = (Long) document.getFields().get(IndexFields.FIELD_BACKUP_AT);
        Long jobid = Long.valueOf((String) document.getFields().get(IndexFields.FIELD_JOB_ID));
        this.client.deleteRecordsForUserAndJobAndTimestamp(jobid, new Date(timestamp));
    }

    private void assertHasNoDocuments() {
        SearchResultAccumulator result = this.client.queryBackup("*", null, null, null, null, null, "username", null, null);
        //TODO PK,AL not the proper asserts here
        assertTrue(result.getFiles().isEmpty());
    }

}
