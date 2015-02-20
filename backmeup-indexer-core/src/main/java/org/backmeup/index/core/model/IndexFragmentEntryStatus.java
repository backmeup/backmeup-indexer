package org.backmeup.index.core.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A DB record on the status of a specific IndexDocument regarding its status in ElasticSearch e.g. if it is waiting for
 * import or if it has been imported/deleted, waiting for import/deletion
 *
 * We only keep a record to the IndexDocuments UUID as for security reasons, the content itself remains within the
 * encrypted user space and not the DB
 */
@Entity
public class IndexFragmentEntryStatus {

    public enum StatusType {
        WAITING_FOR_IMPORT, IMPORTED, WAITING_FOR_DELETION, DELETED;
    }

    @Id
    @GeneratedValue
    private Long Id;
    @Enumerated(EnumType.STRING)
    private StatusType statusType;
    private String documentUUID;
    private Long userID;
    // Timestamp created and last updated
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    public IndexFragmentEntryStatus() {
        this.timestamp = new Date();
    }

    public IndexFragmentEntryStatus(StatusType statusType, String documentUUID, Long userID) {
        this.statusType = statusType;
        this.documentUUID = documentUUID;
        this.userID = userID;
        this.timestamp = new Date();
    }

    public StatusType getStatusType() {
        return this.statusType;
    }

    public void setStatusType(StatusType statusType) {
        this.statusType = statusType;
        this.timestamp = new Date();
    }

    public String getDocumentUUID() {
        return this.documentUUID;
    }

    public void setDocumentUUID(String documentUUID) {
        this.documentUUID = documentUUID;
        this.timestamp = new Date();
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Long getId() {
        return this.Id;
    }

    public void setId(Long id) {
        this.Id = id;
    }

    public Long getUserID() {
        return this.userID;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
        this.timestamp = new Date();
    }

}
