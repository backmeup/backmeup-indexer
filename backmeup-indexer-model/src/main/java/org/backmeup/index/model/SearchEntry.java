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
    private Long datasourceId;
    private String jobName;
    private String preview;
    private final Map<String, String> properties = new HashMap<>();

    public SearchEntry() {
    }

    public SearchEntry(String fileId, Date timeStamp, String type, String title, String thumbnailUrl, String datasource, String jobName) {
        this.fileId = fileId;
        this.timeStamp = timeStamp;
        this.title = title;
        this.setType(type);
        this.thumbnailUrl = thumbnailUrl;
        this.datasource = datasource;
        this.jobName = jobName;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public Set<String> getPropertyKeys() {
        return properties.keySet();
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String filename) {
        this.title = filename;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String id) {
        this.fileId = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getPreviewSnippet() {
        return preview;
    }

    public void setPreviewSnippet(String preview) {
        this.preview = preview;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Long getDatasourceId() {
        return datasourceId;
    }

    public void setDatasourceId(Long datasourceId) {
        this.datasourceId = datasourceId;
    }

    public void copyProperty(String key, Map<String, Object> source) {
        if (source.get(key) != null) {
        	setProperty(key, source.get(key).toString());
        }
    }

}