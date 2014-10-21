package org.backmeup.index.client;

import java.util.HashMap;
import java.util.Map;

/**
 * A document to put into index.
 */
public class IndexDocument {

    private final Map<String, Object> fields = new HashMap<>();
    private final Map<String, String> largeFields = new HashMap<>();

    public void field(String key, Object value) {
        fields.put(key, value);
    }

    public void largeField(String key, String value) {
        largeFields.put(key, value);
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public Map<String, String> getLargeFields() {
        return largeFields;
    }

}
