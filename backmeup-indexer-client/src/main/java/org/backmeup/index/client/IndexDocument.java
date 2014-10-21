package org.backmeup.index.client;

import java.io.IOException;

import org.elasticsearch.common.text.StringText;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 * A document to put into index.
 */
public class IndexDocument {

    private final XContentBuilder contentBuilder;

    public IndexDocument() throws IOException {
        contentBuilder = XContentFactory.jsonBuilder().startObject();
    }

    public void field(String key, Long value) throws IOException {
        contentBuilder.field(key, value);
    }

    public void field(String key, String value) throws IOException {
        contentBuilder.field(key, value);
    }

    public void longField(String key, String value) throws IOException {
        contentBuilder.field(key, new StringText(value));
    }

    public XContentBuilder asElastic() throws IOException {
        contentBuilder.endObject();
        return contentBuilder;
    }

}
