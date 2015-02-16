package org.backmeup.index.core.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.serializer.Json;
import org.hibernate.annotations.Type;

@Entity
public class QueuedIndexDocument {

    @Id
    @GeneratedValue
    private Long queuedId;

    // Timestamp when IndexDocument was added to queue
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    /**
     * we're using a string serealized index document here. This prevents including the org.hibernate.javax.persistence
     * API dependency into the index-model, which the plugins rely on
     **/
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String serealizedIndexDocument;

    public QueuedIndexDocument() {
        //Hibernate constructor
        this.timestamp = new Date();
    }

    public QueuedIndexDocument(IndexDocument doc) {
        this.timestamp = new Date();
        this.serealizedIndexDocument = Json.serialize(doc);
    }

    public Long getQueuedId() {
        return this.queuedId;
    }

    public void setQueuedId(Long queuedId) {
        this.queuedId = queuedId;
    }

    /**
     * String Serialized Version of the IndexDocument
     * 
     * @return
     */
    public String getSerealizedIndexDocument() {
        return this.serealizedIndexDocument;
    }

    /**
     * @param indexDocument
     *            String Serialized Version of the IndexDocument
     */
    public void setSerealizedIndexDocument(String indexDocument) {
        this.serealizedIndexDocument = indexDocument;
    }

    public IndexDocument getIndexDocument() {
        return Json.deserialize(this.serealizedIndexDocument, IndexDocument.class);
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
