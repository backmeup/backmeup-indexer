package org.backmeup.index.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.backmeup.index.api.IndexFields;

public class SearchEntry {

    private String fileId;
    private String ownerId; //the user/sharingpartner that provides this record 
    private boolean isSharing;
    private Date timeStamp;
    private String title;
    private String type;
    private String thumbnailUrl;
    private String downloadUrl;
    private String datasource;
    private String datasourceId;
    private String datasink;
    private String datasinkId;
    private String jobName;
    private String preview;
    private Map<String, String> properties = new HashMap<>();
    private Map<String, String> metadata = new HashMap<>();

    public SearchEntry() {
    }

    public SearchEntry(String fileId, String ownerId, boolean isSharing, Date timeStamp, String type, String title,
            String downloadUrl, String thumbnailUrl, String datasource, String datasink, String jobName,
            String preview, Map<String, String> properties, Map<String, String> metadata) {
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.isSharing = isSharing;
        this.timeStamp = timeStamp;
        this.title = title;
        this.setType(type);
        this.thumbnailUrl = thumbnailUrl;
        this.downloadUrl = downloadUrl;
        this.datasource = datasource;
        this.datasink = datasink;
        this.jobName = jobName;
        this.preview = preview;
        this.properties = properties;
        this.metadata = metadata;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getProperty(String key) {
        return this.properties.get(key);
    }

    public void setProperty(String key, String value) {
        this.properties.put(key, value);
    }

    public Set<String> getPropertyKeys() {
        return this.properties.keySet();
    }

    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getMetadata(String key) {
        return this.metadata.get(key);
    }

    public void setMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    public Set<String> getMetadataKeys() {
        return this.metadata.keySet();
    }

    public Date getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String filename) {
        this.title = filename;
    }

    public String getThumbnailUrl() {
        return this.thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getFileId() {
        return this.fileId;
    }

    public void setFileId(String id) {
        this.fileId = id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDatasource() {
        return this.datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getDatasink() {
        return this.datasink;
    }

    public void setDatasink(String datasink) {
        this.datasink = datasink;
    }

    public String getPreview() {
        return this.preview;
    }

    public void setPreview(String preview) {
        this.preview = preview;
    }

    public String getJobName() {
        return this.jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getDatasourceId() {
        return this.datasourceId;
    }

    public void setDatasourceId(String datasourceId) {
        this.datasourceId = datasourceId;
    }

    public String getDatasinkId() {
        return this.datasinkId;
    }

    public void setDatasinkId(String datasinkId) {
        this.datasinkId = datasinkId;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String userId) {
        this.ownerId = userId;
    }

    public boolean getIsSharing() {
        return this.isSharing;
    }

    public void setIsSharing(boolean isSharing) {
        this.isSharing = isSharing;
    }

    public void copyPropertyIfExist(String key, Map<String, Object> source) {
        if (source.get(key) != null) {
            setProperty(key, source.get(key).toString());
        }
    }

    /**
     * Fetches the Tika metadata records out of all properties within the index
     * 
     * @param source
     */
    public void copyTikaMetadataIfExist(Map<String, Object> source) {
        for (String key : source.keySet()) {
            if (key.startsWith(IndexFields.TIKA_FIELDS_PREFIX)) {
                //return a record without the Tika prefix as key
                String k = key.substring(IndexFields.TIKA_FIELDS_PREFIX.length());
                setMetadata(k, source.get(key).toString());
            }
        }
    }

    @Override
    public String toString() {
        return "SearchEntry [fileId=" + this.fileId + ", ownerId=" + this.ownerId + ", isSahring=" + this.isSharing
                + ", timeStamp=" + this.timeStamp + ", title=" + this.title + ", type=" + this.type + ", thumbnailUrl="
                + this.thumbnailUrl + ", datasource=" + this.datasource + ", datasourceId=" + this.datasourceId
                + ", datasink=" + this.datasink + ", datasinkId=" + this.datasinkId + ", jobName=" + this.jobName
                + ", preview=" + this.preview + ", properties=[" + this.properties + "], metadata=[" + this.metadata
                + "]";
    }

}
