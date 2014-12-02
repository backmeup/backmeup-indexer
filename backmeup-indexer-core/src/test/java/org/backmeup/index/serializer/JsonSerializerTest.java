package org.backmeup.index.serializer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.junit.Test;

public class JsonSerializerTest {

    @Test
    public void shouldPreserveLongs() throws IOException {
        IndexDocument document = deserialize();
        assertEquals(16384L, document.getFields().get(IndexFields.FIELD_OWNER_ID));
        // TODO PK deserialized does not look too good, has double instead of int/long
    }

    @Test
    public void shouldPreserveStrings() throws IOException {
        IndexDocument document = deserialize();
        assertEquals("john.doe", document.getFields().get(IndexFields.FIELD_OWNER_NAME));
    }

    private IndexDocument deserialize() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("sampleIndexDocument.serindexdocument")) {
            String json = IOUtils.toString(resource);
            return JsonSerializer.deserialize(json, IndexDocument.class);
        }
    }

    // TODO PK Test UUID

}
