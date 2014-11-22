package org.backmeup.index.model;

public class FileInfo {

    private String fileId;
    private String source;
    private Long sourceId;
    private long timeStamp;
    private String title;
    private String type;
    private String thumbnailURL;
    private String path;
    private String sink;

    public FileInfo() {
    }

    public FileInfo(String fileId, String source, long timeStamp, String title, String type, String thumbnailURL) {
        this.fileId = fileId;
        this.source = source;
        this.timeStamp = timeStamp;
        this.title = title;
        this.type = type;
        this.thumbnailURL = thumbnailURL;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getThumbnailURL() {
        return thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSink(String sink) {
        this.sink = sink;
    }

    public String getSink() {
        return this.sink;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public String toString() {
        return "FileInfo [fileId=" + fileId + ", source=" + source + ", sourceId=" + sourceId + ", timeStamp=" + timeStamp + ", title="
                + title + ", type=" + type + ", thumbnailURL=" + thumbnailURL + ", path=" + path + ", sink=" + sink + "]";
    }

}