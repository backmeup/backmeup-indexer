package org.backmeup.index.model;

import java.util.List;

public class SearchResultAccumulator {

    private List<CountedEntry> bySource;
    private List<CountedEntry> byType;
    private List<CountedEntry> byJob;
    private List<SearchEntry> files;

    public List<CountedEntry> getBySource() {
        return bySource;
    }

    public void setBySource(List<CountedEntry> bySource) {
        this.bySource = bySource;
    }

    public List<CountedEntry> getByType() {
        return byType;
    }

    public void setByType(List<CountedEntry> byType) {
        this.byType = byType;
    }

    public List<CountedEntry> getByJob() {
        return byJob;
    }

    public void setByJob(List<CountedEntry> byJob) {
        this.byJob = byJob;
    }

    public List<SearchEntry> getFiles() {
        return files;
    }

    public void setFiles(List<SearchEntry> files) {
        this.files = files;
    }

}