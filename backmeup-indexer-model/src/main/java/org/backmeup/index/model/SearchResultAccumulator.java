package org.backmeup.index.model;

import java.util.List;

public class SearchResultAccumulator {

    private List<CountedEntry> bySource;
    private List<CountedEntry> byType;
    private List<CountedEntry> byJob;
    private List<SearchEntry> files;
    private List<CountedEntry> byOwner;
    private List<CountedEntry> byTag;
    //elements that are related to the paging and offset for search results
    private Long offsetStart;
    private Long offsetEnd;

    public List<CountedEntry> getBySource() {
        return this.bySource;
    }

    public void setBySource(List<CountedEntry> bySource) {
        this.bySource = bySource;
    }

    public List<CountedEntry> getByType() {
        return this.byType;
    }

    public void setByType(List<CountedEntry> byType) {
        this.byType = byType;
    }

    public List<CountedEntry> getByJob() {
        return this.byJob;
    }

    public void setByJob(List<CountedEntry> byJob) {
        this.byJob = byJob;
    }

    public List<CountedEntry> getByOwner() {
        return this.byOwner;
    }

    public void setByOwner(List<CountedEntry> byOwner) {
        this.byOwner = byOwner;
    }

    public List<CountedEntry> getByTag() {
        return this.byTag;
    }

    public void setByTag(List<CountedEntry> byTag) {
        this.byTag = byTag;
    }

    public List<SearchEntry> getFiles() {
        return this.files;
    }

    public void setFiles(List<SearchEntry> files) {
        this.files = files;
    }

    public Long getOffsetStart() {
        return this.offsetStart;
    }

    public void setOffsetStart(Long offsetStart) {
        this.offsetStart = offsetStart;
    }

    public Long getOffsetEnd() {
        return this.offsetEnd;
    }

    public void setOffsetEnd(Long offsetEnd) {
        this.offsetEnd = offsetEnd;
    }

}