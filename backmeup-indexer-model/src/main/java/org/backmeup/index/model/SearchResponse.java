package org.backmeup.index.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Contains the result of a search when calling BusinessLogic#queryBackup.
 */
public class SearchResponse {

    private int progress;

    private String query;

    private String filters;

    private SearchResultAccumulator searchResults = new SearchResultAccumulator();

    //elements that are related to the paging and offset for search results
    private Long offsetStart;
    private Long offsetEnd;

    public SearchResponse() {
    }

    public SearchResponse(String query) {
        this(query, new ArrayList<String>());
    }

    static String join(Collection<?> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    public SearchResponse(String query, List<String> filters) {
        this.query = query;
        this.setFilters(join(filters, ","));
    }

    public SearchResponse(int status, String query, List<SearchEntry> files) {
        this(status, query, files, null, null, null, null, null);
    }

    public SearchResponse(int status, String query, List<CountedEntry> bySource, List<CountedEntry> byType,
            List<CountedEntry> byJob, List<CountedEntry> byOwner, List<CountedEntry> byTag) {
        this(status, query, null, bySource, byType, byJob, byOwner, byTag);
    }

    public SearchResponse(int status, String query, List<SearchEntry> files, List<CountedEntry> bySource,
            List<CountedEntry> byType, List<CountedEntry> byJob, List<CountedEntry> byOwner, List<CountedEntry> byTag) {
        this.progress = status;
        this.query = query;
        setFiles(files);
        setBySource(bySource);
        setByType(byType);
        setByJob(byJob);
        setByOwner(byOwner);
        setByTag(byTag);
    }

    public String getQuery() {
        return this.query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setDetails(SearchResultAccumulator searchResults) {
        this.searchResults = searchResults;
        this.setOffsetStart(searchResults.getOffsetStart());
        this.setOffsetEnd(searchResults.getOffsetEnd());
    }

    public List<CountedEntry> getBySource() {
        return this.searchResults.getBySource();
    }

    public void setBySource(List<CountedEntry> bySource) {
        this.searchResults.setBySource(bySource);
    }

    public List<CountedEntry> getByType() {
        return this.searchResults.getByType();
    }

    public void setByType(List<CountedEntry> byType) {
        this.searchResults.setByType(byType);
    }

    public List<CountedEntry> getByJob() {
        return this.searchResults.getByJob();
    }

    public void setByJob(List<CountedEntry> byJob) {
        this.searchResults.setByJob(byJob);
    }

    public List<CountedEntry> getByOwner() {
        return this.searchResults.getByOwner();
    }

    public void setByOwner(List<CountedEntry> byOwner) {
        this.searchResults.setByOwner(byOwner);
    }

    public List<CountedEntry> getByTag() {
        return this.searchResults.getByTag();
    }

    public void setByTag(List<CountedEntry> byTag) {
        this.searchResults.setByTag(byTag);
    }

    public int getProgress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public List<SearchEntry> getFiles() {
        return this.searchResults.getFiles();
    }

    public void setFiles(List<SearchEntry> files) {
        this.searchResults.setFiles(files);
    }

    public String getFilters() {
        return this.filters;
    }

    public void setFilters(String filters) {
        this.filters = filters;
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
