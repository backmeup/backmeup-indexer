package org.backmeup.index.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Contains the result of a search when calling BusinessLogic#queryBackup.
 * 
 */
public class SearchResponse implements SearchResultAccumulator {
	
	private long id;
	
	private int progress;
	
	private String query;
	
	private String filters;
	
	private List<SearchEntry> files;
	
	private List<CountedEntry> bySource;
	
	private List<CountedEntry> byType;
	
	private List<CountedEntry> byJob;
	
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
	
	public SearchResponse(long id, int status, String query, List<SearchEntry> files) {
		this(id, status, query, files, null, null, null);
	}
	
	public SearchResponse(long id, int status, String query, List<CountedEntry> bySource, List<CountedEntry> byType, List<CountedEntry> byJob) {
		this(id, status, query, null, bySource, byType, byJob);
	}
	
	public SearchResponse(long id, int status, String query, List<SearchEntry> files, List<CountedEntry> bySource, List<CountedEntry> byType, List<CountedEntry> byJob) {
		this.id = id;
		this.progress = status;
		this.query = query;
		this.files = files;
		this.bySource = bySource;
		this.byType = byType;
		this.byJob = byJob;
	}
	
	public String getQuery() {
		return query;
	}
	
	public void setQuery(String query) {
		this.query = query;
	}
	
	public List<CountedEntry> getBySource() {
		return bySource;
	}

	@Override
    public void setBySource(List<CountedEntry> bySource) {
		this.bySource = bySource;
	}

	public List<CountedEntry> getByType() {
		return byType;
	}

	@Override
    public void setByType(List<CountedEntry> byType) {
		this.byType = byType;
	}
	
	public List<CountedEntry> getByJob() {
		return byJob;
	}

	@Override
    public void setByJob(List<CountedEntry> byJob) {
		this.byJob = byJob;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	
	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public List<SearchEntry> getFiles() {
		return files;
	}

	@Override
    public void setFiles(List<SearchEntry> files) {
		this.files = files;
	} 
	
	public String getFilters() {
		return filters;
	}

	public void setFilters(String filters) {
		this.filters = filters;
	}
}
