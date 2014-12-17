package org.backmeup.index.model;

public class FileInfo {

    private String fileId;
    private String source;
    private String sourceId;
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
        return this.fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getThumbnailURL() {
        return this.thumbnailURL;
    }

    public void setThumbnailURL(String thumbnailURL) {
        this.thumbnailURL = thumbnailURL;
    }

    public String getPath() {
        return this.path;
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

    public String getSourceId() {
        return this.sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public String toString() {
        return "FileInfo [fileId=" + this.fileId + ", source=" + this.source + ", sourceId=" + this.sourceId
                + ", timeStamp=" + this.timeStamp + ", title=" + this.title + ", type=" + this.type + ", thumbnailURL="
                + this.thumbnailURL + ", path=" + this.path + ", sink=" + this.sink + "]";
    }

}