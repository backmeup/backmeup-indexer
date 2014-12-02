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
    }

    @Test
    public void shouldPreserveStrings() throws IOException {
        IndexDocument document = deserialize();
        assertEquals("john.doe", document.getFields().get(IndexFields.FIELD_OWNER_NAME));
    }

    @Test
    public void shouldDeserialiseLargeFields() throws IOException {
        IndexDocument document = deserialize();
        assertEquals("b", document.getLargeFields().get("a"));
    }

    @Test
    public void shouldBeTheSameAfterFullRoundTrip() throws IOException {
        IndexDocument document = deserialize();
        IndexDocument againDeserialized = deserialize(JsonSerializer.serialize(document));
        assertEquals(document.getFields(), againDeserialized.getFields());
        assertEquals(document.getLargeFields(), againDeserialized.getLargeFields());
    }

    private IndexDocument deserialize() throws IOException {
        return deserialize(load());
    }

    private IndexDocument deserialize(String json) {
        return JsonSerializer.deserialize(json, IndexDocument.class);
    }

    private String load() throws IOException {
        try (InputStream resource = getClass().getClassLoader().getResourceAsStream("sampleIndexDocument.serindexdocument")) {
            return IOUtils.toString(resource);
        }
    }

}
