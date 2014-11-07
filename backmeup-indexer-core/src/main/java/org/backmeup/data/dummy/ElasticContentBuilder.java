package org.backmeup.data.dummy;

import java.io.IOException;
import java.util.Map;

import org.backmeup.index.model.IndexDocument;
import org.elasticsearch.common.text.StringText;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class ElasticContentBuilder {

	private final IndexDocument document;
	private final XContentBuilder contentBuilder;

	public static final String INDEX_NAME = "backmeup";
	public static final String DOCUMENT_TYPE_BACKUP = "backup";

	public ElasticContentBuilder(IndexDocument document) throws IOException {
		this.document = document;
		this.contentBuilder = XContentFactory.jsonBuilder().startObject();
	}

	public XContentBuilder asElastic() throws IOException {
		copyFields();
		copyLargeFields();
		this.contentBuilder.endObject();
		return this.contentBuilder;
	}

	private void copyFields() throws IOException {
		for (Map.Entry<String, Object> entry : this.document.getFields()
				.entrySet()) {
			field(entry);
		}
	}

	private void field(Map.Entry<String, Object> entry) throws IOException {
		this.contentBuilder.field(entry.getKey(), entry.getValue());
	}

	private void copyLargeFields() throws IOException {
		for (Map.Entry<String, String> entry : this.document.getLargeFields()
				.entrySet()) {
			largeField(entry);
		}
	}

	private void largeField(Map.Entry<String, String> entry) throws IOException {
		this.contentBuilder.field(entry.getKey(),
				new StringText(entry.getValue()));
	}
}
