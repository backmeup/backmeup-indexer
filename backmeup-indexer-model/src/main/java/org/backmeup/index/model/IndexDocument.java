package org.backmeup.index.model;

import java.io.Serializable;
import java.util.Collections;
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

    // can not allow any value because in the map we lose the type information used for serialisation. So no other types possible.
    public void field(String key, Long value) {
        this.fields.put(key, value);
    }

    public void field(String key, String value) {
        this.fields.put(key, value);
    }

    public void largeField(String key, String value) {
        this.largeFields.put(key, value);
    }

    /**
     * Read-only map of entries (Long and Strings).
     */
    public Map<String, Object> getFields() {
        return Collections.unmodifiableMap(this.fields);
    }

    /**
     * Read-only map of large entries (Strings).
     */
    public Map<String, String> getLargeFields() {
        return Collections.unmodifiableMap(this.largeFields);
    }

    // --- setters for JSON serialisation

    public void setLargeFields(Map<String, String> largeFields) {
        this.largeFields = largeFields;
    }
}
