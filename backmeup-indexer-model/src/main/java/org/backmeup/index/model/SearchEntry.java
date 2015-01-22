package org.backmeup.index.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SearchEntry {

    private String fileId;
    private Date timeStamp;
    private String title;
    private String type;
    private String thumbnailUrl;
    private String datasource;
    private String datasourceId;
    private String jobName;
    private String preview;
    private Map<String, String> properties = new HashMap<>();

    public SearchEntry() {
    }

    public SearchEntry(String fileId, Date timeStamp, String type, String title, String thumbnailUrl,
            String datasource, String jobName, String preview, Map<String, String> properties) {
        this.fileId = fileId;
        this.timeStamp = timeStamp;
        this.title = title;
        this.setType(type);
        this.thumbnailUrl = thumbnailUrl;
        this.datasource = datasource;
        this.jobName = jobName;
        this.preview = preview;
        this.properties = properties;
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

    public void copyPropertyIfExist(String key, Map<String, Object> source) {
        if (source.get(key) != null) {
            setProperty(key, source.get(key).toString());
        }
    }

    @Override
    public String toString() {
        return "SearchEntry [fileId=" + this.fileId + ", timeStamp=" + this.timeStamp + ", title=" + this.title
                + ", type=" + this.type + ", thumbnailUrl=" + this.thumbnailUrl + ", datasource=" + this.datasource
                + ", datasourceId=" + this.datasourceId + ", jobName=" + this.jobName + ", preview=" + this.preview
                + ", properties=" + this.properties + "]";
    }

}
