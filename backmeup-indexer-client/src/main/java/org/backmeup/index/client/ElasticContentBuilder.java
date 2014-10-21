package org.backmeup.index.client;

import java.io.IOException;
import java.util.Map;

import org.elasticsearch.common.text.StringText;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

class ElasticContentBuilder {

    private final IndexDocument document;
    private final XContentBuilder contentBuilder;

    public ElasticContentBuilder(IndexDocument document) throws IOException {
        this.document = document;
        contentBuilder = XContentFactory.jsonBuilder().startObject();
    }

    public XContentBuilder asElastic() throws IOException {
        copyFields();
        copyLargeFields();
        contentBuilder.endObject();
        return contentBuilder;
    }

    private void copyFields() throws IOException {
        for (Map.Entry<String, Object> entry : document.getFields().entrySet()) {
            field(entry);
        }
    }

    private void field(Map.Entry<String, Object> entry) throws IOException {
        contentBuilder.field(entry.getKey(), entry.getValue());
    }

    private void copyLargeFields() throws IOException {
        for (Map.Entry<String, String> entry : document.getLargeFields().entrySet()) {
            largeField(entry);
        }
    }

    private void largeField(Map.Entry<String, String> entry) throws IOException {
        contentBuilder.field(entry.getKey(), new StringText(entry.getValue()));
    }
}
