package org.backmeup.index.model;

import java.util.List;

public interface SearchResultAccumulator {

    void setBySource(List<CountedEntry> bySource);

    void setByType(List<CountedEntry> byType);

    void setByJob(List<CountedEntry> byJob);

    void setFiles(List<SearchEntry> files);

}