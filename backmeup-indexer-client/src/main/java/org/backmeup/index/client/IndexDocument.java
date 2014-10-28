package org.backmeup.index.client;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A document to put into index.
 */
@XmlRootElement
public class IndexDocument {

    private Map<String, Object> fields = new HashMap<>();
    private Map<String, String> largeFields = new HashMap<>();

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

    // --- setters for JSON serialisation

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public void setLargeFields(Map<String, String> largeFields) {
        this.largeFields = largeFields;
    }
}
