package org.backmeup.data.dummy;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.backmeup.index.IndexManager;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticSearchIndexClient implements IndexClient {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String INDEX_NAME = "backmeup";
    private static final String DOCUMENT_TYPE_BACKUP = "backup";

    private final User userId;
    private final Client client;

    public ElasticSearchIndexClient(User userId, IndexManager indexManager) {
        this(userId, indexManager.initAndCreateAndDoEverthing(userId.id()));
    }

    public ElasticSearchIndexClient(User userId, Client client) {
        this.userId = userId;
        this.client = client;
        createIndexIfNeeded();
    }

    private void createIndexIfNeeded() {
        IndicesExistsResponse existsResponse = this.client.admin().indices().prepareExists(INDEX_NAME).execute()
                .actionGet();
        if (!existsResponse.isExists()) {
            CreateIndexRequestBuilder cirb = this.client.admin().indices().prepareCreate(INDEX_NAME);
            CreateIndexResponse createIndexResponse = cirb.execute().actionGet();
            if (!createIndexResponse.isAcknowledged()) {
                // throw new Exception("Could not create index ["+ INDEX_NAME +"].");
                this.logger.info("Could not create index [" + INDEX_NAME + " ].");
            }
        }
    }

    @Override
    public SearchResultAccumulator queryBackup(String query, String source, String type, String job, String username) {
        Map<String, List<String>> filters = createFiltersFor(source, type, job);
        return queryBackup(query, filters, username);
    }

    private Map<String, List<String>> createFiltersFor(String source, String type, String job) {
        Map<String, List<String>> filters = null;

        if (source != null || type != null || job != null) {
            filters = new HashMap<>();
        }

        if (source != null) {
            List<String> filtervalue = new LinkedList<>();
            filtervalue.add(source);
            filters.put("source", filtervalue);
        }

        if (type != null) {
            List<String> filtervalue = new LinkedList<>();
            filtervalue.add(type);
            filters.put("type", filtervalue);
        }

        if (job != null) {
            List<String> filtervalue = new LinkedList<>();
            filtervalue.add(job);
            filters.put("job", filtervalue);
        }

        return filters;
    }

    public SearchResultAccumulator queryBackup(String query, Map<String, List<String>> filters, String username) {
        SearchResponse esResponse = queryBackup(query, filters);
        SearchResultAccumulator result = new SearchResultAccumulator();
        result.setFiles(IndexUtils.convertSearchEntries(esResponse, username));
        result.setBySource(IndexUtils.getBySource(esResponse));
        result.setByType(IndexUtils.getByType(esResponse));
        result.setByJob(IndexUtils.getByJob(esResponse));
        return result;
    }

    private SearchResponse queryBackup(String query, Map<String, List<String>> filters) {
        String queryString = buildQuery(query);

        /*
         * QueryBuilder qBuilder = QueryBuilders.queryString(queryString);
         */

        QueryBuilder qBuilder = IndexUtils.buildQuery(this.userId, queryString, filters);
        this.logger.debug("#######################################");
        this.logger.debug("QueryString:\n" + qBuilder.toString());
        this.logger.debug("#######################################");

        return this.client.prepareSearch(INDEX_NAME).setQuery(qBuilder).addSort("backup_at", SortOrder.DESC)
                .addHighlightedField(IndexFields.FIELD_FULLTEXT).setSize(100).execute().actionGet();
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
                for (int i = 0; i < tokens.length; i++) {
                    sb.append(tokens[i]);
                    if (i < tokens.length - 1) {
                        sb.append("* AND *");
                    }
                }
                queryString = sb.toString() + "*";
            }
        }

        // queryString = IndexUtils.getFilterSuffix(filters) + "owner_id:" +
        // user.getUserId() + " AND " + queryString;
        // logger.debug("QueryString = " + queryString);

        return queryString;
    }

    @Override
    public Set<FileItem> searchAllFileItemsForJob(Long jobId) {
        SearchResponse esResponse = searchByJobId(jobId);
        return IndexUtils.convertToFileItems(esResponse);
    }

    private SearchResponse searchByJobId(long jobId) {
        QueryBuilder qBuilder = QueryBuilders.matchQuery(IndexFields.FIELD_JOB_ID, jobId);
        return this.client.prepareSearch(INDEX_NAME).setQuery(qBuilder).execute().actionGet();
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

        return this.client.prepareSearch(INDEX_NAME).setQuery(qBuilder).execute().actionGet();
    }

    @Override
    public FileInfo getFileInfoForFile(String fileId) {
        SearchResponse esResponse = getFileById(fileId);
        return IndexUtils.convertToFileInfo(esResponse);
    }

    @Override
    public String getThumbnailPathForFile(String fileId) {
        SearchResponse response = getFileById(fileId);
        SearchHit hit = response.getHits().getHits()[0];
        Map<String, Object> source = hit.getSource();
        return source.get(IndexFields.FIELD_THUMBNAIL_PATH).toString();
    }

    @Override
    public void deleteRecordsForUser() {
        boolean hasIndex = this.client.admin().indices().exists(new IndicesExistsRequest("indexName")).actionGet()
                .isExists();
        if (hasIndex) {
            this.client.prepareDeleteByQuery(INDEX_NAME)
                    .setQuery(QueryBuilders.matchQuery(IndexFields.FIELD_OWNER_ID, this.userId)).execute().actionGet();
        }
    }

    @Override
    public void deleteRecordsForJobAndTimestamp(Long jobId, Long timestamp) {
        QueryBuilder qBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery(IndexFields.FIELD_JOB_ID, jobId))
                .must(QueryBuilders.matchQuery(IndexFields.FIELD_BACKUP_AT, timestamp));

        this.client.prepareDeleteByQuery(INDEX_NAME).setQuery(qBuilder).execute().actionGet();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.close();
        }
    }

    @Override
    public void index(IndexDocument document) throws IOException {
        this.logger.debug("Pushing to ES index...");
        XContentBuilder elasticBuilder = new ElasticContentBuilder(document).asElastic();
        this.client.prepareIndex(INDEX_NAME, DOCUMENT_TYPE_BACKUP).setSource(elasticBuilder).setRefresh(true).execute()
                .actionGet();
        this.logger.debug(" done.");
    }

    //TODO UPDATE TTL SERVICE
}
