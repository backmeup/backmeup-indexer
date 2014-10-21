package org.backmeup.index.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.SearchResultAccumulator;
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
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final String INDEX_NAME = "backmeup";
	private static final String CLUSTER_NAME = "es-backmeup-cluster";
    private static final String DOCUMENT_TYPE_BACKUP = "backup";

	private final Long userId;
	private final Client client;

    public ElasticSearchIndexClient(Long userId) {
        this(userId, createClusterTransferClient());
    }

    private static TransportClient createClusterTransferClient() {
        // host = NetworkUtils.getLocalAddress().getHostName();
        // Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", "es-cluster-" + NetworkUtils.getLocalAddress().getHostName()).build();
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", CLUSTER_NAME).build();
        return new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("host", 9999));
    }

    public ElasticSearchIndexClient(Long userId, Client client) {
        this.userId = userId;
        this.client = client;
        createIndexIfNeeded();
    }

    private void createIndexIfNeeded() {
        IndicesExistsResponse existsResponse = client.admin().indices().prepareExists(INDEX_NAME).execute().actionGet();
        if (!existsResponse.isExists()) {
            CreateIndexRequestBuilder cirb = client.admin().indices().prepareCreate(INDEX_NAME);
            CreateIndexResponse createIndexResponse = cirb.execute().actionGet();
            if (!createIndexResponse.isAcknowledged()) {
//              throw new Exception("Could not create index ["+ INDEX_NAME +"].");
                logger.info("Could not create index [" + INDEX_NAME +" ].");
            }
        }
    }
	
    public void queryBackup(String query, Map<String, List<String>> filters, String username, SearchResultAccumulator result) {
        SearchResponse esResponse = queryBackup(query, filters);
        result.setFiles(IndexUtils.convertSearchEntries(esResponse, username));
        result.setBySource(IndexUtils.getBySource(esResponse));
        result.setByType(IndexUtils.getByType(esResponse));
        result.setByJob(IndexUtils.getByJob(esResponse));
    }
	
	private SearchResponse queryBackup(String query, Map<String, List<String>> filters) {		
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
				.addHighlightedField(IndexFields.FIELD_FULLTEXT)
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
	
    public Set<FileItem> searchAllFileItemsForJob(Long jobId) {
        SearchResponse esResponse = searchByJobId(jobId);
        return IndexUtils.convertToFileItems(esResponse);
    }

	private SearchResponse searchByJobId(long jobId) {
		QueryBuilder qBuilder = QueryBuilders.matchQuery(IndexFields.FIELD_JOB_ID, jobId);
		return client.prepareSearch(INDEX_NAME).setQuery(qBuilder).execute().actionGet();
	}
	
	private SearchResponse getFileById(String fileId) {
		// IDs in backmeup are "owner:hash:timestamp"
		String[] bmuId = fileId.split(":");
		if (bmuId.length != 3) {
			throw new IllegalArgumentException("Invalid file ID: " + fileId);
		}
		
		Long owner = Long.parseLong(bmuId[0]);
		String hash = bmuId[1];
		Long timestamp = Long.parseLong(bmuId[2]);
		
		QueryBuilder qBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.matchQuery(IndexFields.FIELD_OWNER_ID, owner))
				.must(QueryBuilders.matchQuery(IndexFields.FIELD_FILE_HASH, hash))
				.must(QueryBuilders.matchQuery(IndexFields.FIELD_BACKUP_AT, timestamp));
		
			return client.prepareSearch(INDEX_NAME).setQuery(qBuilder).execute().actionGet();
	}

    public FileInfo getFileInfoForFile(String fileId) {
        SearchResponse esResponse = getFileById(fileId);
        return IndexUtils.convertToFileInfo(esResponse);
    }
    
	public String getThumbnailPathForFile(String fileId) {
		SearchResponse response = getFileById(fileId);
		SearchHit hit = response.getHits().getHits()[0];
		Map<String, Object> source = hit.getSource();
		return source.get(IndexFields.FIELD_THUMBNAIL_PATH).toString();
	}
	
	public void deleteRecordsForUser() {
		boolean hasIndex = client.admin().indices().exists(
				new IndicesExistsRequest("indexName")).actionGet().isExists();
		if(hasIndex){
			client.prepareDeleteByQuery(INDEX_NAME)
				.setQuery(QueryBuilders.matchQuery(IndexFields.FIELD_OWNER_ID, userId))
				.execute().actionGet();
		}
	}
	
	public void deleteRecordsForJobAndTimestamp(Long jobId, Long timestamp) {
		QueryBuilder qBuilder = QueryBuilders.boolQuery()
				.must(QueryBuilders.matchQuery(IndexFields.FIELD_JOB_ID, jobId))
				.must(QueryBuilders.matchQuery(IndexFields.FIELD_BACKUP_AT, timestamp));

		client.prepareDeleteByQuery(INDEX_NAME)
			.setQuery(qBuilder).execute().actionGet();
	}
	
	@Override
    public void close() {
		if (client != null) {
			client.close();
		}
	}

	// TODO PK use interface when splitting
    public IndexDocument createDocument() throws IOException {
        return new IndexDocument();
    }

    public void index(IndexDocument contentBuilder) throws IOException {
        logger.debug("Pushing to ES index...");
        client.prepareIndex(INDEX_NAME, DOCUMENT_TYPE_BACKUP).setSource(contentBuilder.asElastic()).setRefresh(true).execute().actionGet();
        logger.debug(" done.");
    }

}
