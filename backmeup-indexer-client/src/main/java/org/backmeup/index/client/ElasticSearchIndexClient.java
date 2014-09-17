package org.backmeup.index.client;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchIndexClient implements Closeable {
	private final Logger logger = LoggerFactory.getLogger(ElasticSearchIndexClient.class);
	
	private static final String INDEX_NAME = "backmeup";
	
	private static final String CLUSTER_NAME = "es-backmeup-cluster";
	
	private final Long userId;
	private final Client client;
	
	public ElasticSearchIndexClient(Long userId) {
		this.userId = userId;
        //host = NetworkUtils.getLocalAddress().getHostName();
		//Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "es-cluster-" + NetworkUtils.getLocalAddress().getHostName()).build();
		Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", CLUSTER_NAME).build();
		client = new TransportClient(settings)
			.addTransportAddress(new InetSocketTransportAddress("host", 9999));
		
		// Check if index exists and if not create it.   
	    IndicesExistsResponse existsResponse = client.admin().indices().prepareExists(INDEX_NAME).execute().actionGet();
        if (!existsResponse.isExists()) {
            CreateIndexRequestBuilder cirb = client.admin().indices().prepareCreate(INDEX_NAME);
            CreateIndexResponse createIndexResponse = cirb.execute().actionGet();
            if (!createIndexResponse.isAcknowledged()) {
//            	throw new Exception("Could not create index ["+ INDEX_NAME +"].");
            	logger.info("Could not create index [" + INDEX_NAME +" ].");
            }
        }
		
	}
	
	public SearchResponse queryBackup(String query) {
		return queryBackup(query, null);
	}
	
	public SearchResponse queryBackup(String query, Map<String, List<String>> filters) {		
		String queryString = buildQuery(query);
		
		/*
		QueryBuilder qBuilder = QueryBuilders.queryString(queryString);
		*/
		
        QueryBuilder qBuilder = IndexUtils.buildQuery (userId, queryString, filters);
		logger.debug("#######################################");
		logger.debug("QueryString:\n" + qBuilder.toString ());
		logger.debug("#######################################");
		
		return client.prepareSearch(INDEX_NAME)
				.setQuery(qBuilder)
				.addSort("backup_at", SortOrder.DESC)
				.addHighlightedField(IndexUtils.FIELD_FULLTEXT)
				.setSize(100)
				.execute().actionGet();
	}

    private String buildQuery(String query) {
        String queryString = null;
		String[] tokens = query.split(" ");
		if (tokens.length == 0) {
			queryString = "*";
		} else if (tokens.length == 1) {
			queryString = query;
		} else {
			if (query.contains("AND") || query.contains("OR")) {
				queryString = query;
			} else {
				StringBuffer sb = new StringBuffer("*");
				for (int i=0; i<tokens.length; i++) {
					sb.append(tokens[i]);
					if (i < tokens.length - 1) {
						sb.append("* AND *");
					}
				}
				queryString = sb.toString() + "*";
			}
		}
		
	    // queryString = IndexUtils.getFilterSuffix(filters) + "owner_id:" + user.getUserId() + " AND " + queryString;
	    // logger.debug("QueryString = " + queryString);

        return queryString;
    }
	
	public SearchResponse searchByJobId(long jobId) {
		QueryBuilder qBuilder = QueryBuilders.matchQuery(IndexUtils.FIELD_JOB_ID, jobId);
		return client.prepareSearch(INDEX_NAME).setQuery(qBuilder).execute().actionGet();
	}
	
	public SearchResponse getFileById(String fileId) {
		// IDs in backmeup are "owner:hash:timestamp"
		String[] bmuId = fileId.split(":");
		if (bmuId.length != 3) {
			throw new IllegalArgumentException("Invalid file ID: " + fileId);
		}
		
		Long owner = Long.parseLong(bmuId[0]);
		String hash = bmuId[1];
		Long timestamp = Long.parseLong(bmuId[2]);
		
		QueryBuilder qBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.matchQuery(IndexUtils.FIELD_OWNER_ID, owner))
				.must(QueryBuilders.matchQuery(IndexUtils.FIELD_FILE_HASH, hash))
				.must(QueryBuilders.matchQuery(IndexUtils.FIELD_BACKUP_AT, timestamp));
		
			return client.prepareSearch(INDEX_NAME).setQuery(qBuilder).execute().actionGet();
	}
	
	public String getThumbnailPathForFile(String fileId) {
		SearchResponse response = getFileById(fileId);
		SearchHit hit = response.getHits().getHits()[0];
		Map<String, Object> source = hit.getSource();
		return source.get(IndexUtils.FIELD_THUMBNAIL_PATH).toString();
	}
	
	public void deleteRecordsForUser() {
		boolean hasIndex = client.admin().indices().exists(
				new IndicesExistsRequest("indexName")).actionGet().isExists();
		if(hasIndex){
			client.prepareDeleteByQuery(INDEX_NAME)
				.setQuery(QueryBuilders.matchQuery(IndexUtils.FIELD_OWNER_ID, userId))
				.execute().actionGet();
		}
	}
	
	public void deleteRecordsForJobAndTimestamp(Long jobId, Long timestamp) {
		QueryBuilder qBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.matchQuery(IndexUtils.FIELD_JOB_ID, jobId))
				.must(QueryBuilders.matchQuery(IndexUtils.FIELD_BACKUP_AT, timestamp));

		client.prepareDeleteByQuery(INDEX_NAME)
			.setQuery(qBuilder).execute().actionGet();
	}
	
	@Override
    public void close() {
		if (client != null) {
			client.close();
		}
	}
}
