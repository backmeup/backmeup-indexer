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
	
	public SearchResponse() { }
	
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
		this(status, query, files, null, null, null);
	}
	
	public SearchResponse(int status, String query, List<CountedEntry> bySource, List<CountedEntry> byType, List<CountedEntry> byJob) {
		this(status, query, null, bySource, byType, byJob);
	}
	
	public SearchResponse(int status, String query, List<SearchEntry> files, List<CountedEntry> bySource, List<CountedEntry> byType, List<CountedEntry> byJob) {
		this.progress = status;
		this.query = query;
		setFiles(files);
		setBySource(bySource);
		setByType(byType);
		setByJob(byJob);
	}
	
	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
    public void setDetails(SearchResultAccumulator searchResults) {
        this.searchResults = searchResults;
    }

	public List<CountedEntry> getBySource() {
        return searchResults.getBySource();
    }

    public void setBySource(List<CountedEntry> bySource) {
        searchResults.setBySource(bySource);
    }

    public List<CountedEntry> getByType() {
        return searchResults.getByType();
    }

    public void setByType(List<CountedEntry> byType) {
        searchResults.setByType(byType);
    }

    public List<CountedEntry> getByJob() {
        return searchResults.getByJob();
    }

    public void setByJob(List<CountedEntry> byJob) {
        searchResults.setByJob(byJob);
    }

    public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

    public List<SearchEntry> getFiles() {
        return searchResults.getFiles();
    }

    public void setFiles(List<SearchEntry> files) {
        searchResults.setFiles(files);
    }

	public String getFilters() {
		return filters;
	}

	public void setFilters(String filters) {
		this.filters = filters;
	}

}
