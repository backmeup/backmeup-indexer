package org.backmeup.index.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A document to put into index.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@XmlRootElement
public class IndexDocument implements Serializable {

    private Map<String, Object> fields = new HashMap<>();
    private Map<String, String> largeFields = new HashMap<>();

    public void field(String key, Object value) {
        this.fields.put(key, value);
    }

    public void largeField(String key, String value) {
        this.largeFields.put(key, value);
    }

    public Map<String, Object> getFields() {
        return this.fields;
    }

    public Map<String, String> getLargeFields() {
        return this.largeFields;
    }

    // --- setters for JSON serialisation

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public void setLargeFields(Map<String, String> largeFields) {
        this.largeFields = largeFields;
    }
}
