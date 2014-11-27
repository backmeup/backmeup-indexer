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
import org.junit.Ignore;
import org.junit.Test;

@Ignore("only integration with running and deployed Indexer WAR")
public class IntegrationTest {

    private static final long USER = 16384;

    @Test
    public void shouldIndexAndQuery() throws IOException {
        IndexClient client = new IndexClientFactory().getIndexClient(USER);

        IndexDocument document = deserialize();
        assertEquals(16384, document.getFields().get(IndexFields.FIELD_OWNER_ID));
        client.index(document);

        SearchResultAccumulator result = client.queryBackup("*", null, null, null, "username");
        assertTrue(result.getFiles().size() > 0);
    }

    private IndexDocument deserialize() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("indexDocument.json")) {
            String json = IOUtils.toString(resource);
            return JsonSerializer.deserialize(json, IndexDocument.class);
            // TODO PK deserialized does not look too good, has double instead of int/long
        }
    }
}
