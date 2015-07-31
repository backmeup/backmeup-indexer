package org.backmeup.index.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.dal.TaggedCollectionDao;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.SearchResultAccumulator;
import org.backmeup.index.model.User;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.index.IndexResponse;
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

    private TaggedCollectionDao taggedCollectionDao;

    public ElasticSearchIndexClient(User userId, Client client, TaggedCollectionDao dao) {
        this(userId, client);
        this.taggedCollectionDao = dao;
    }

    @Deprecated
    public ElasticSearchIndexClient(User userId, Client client) {
        this.userId = userId;
        this.client = client;
        createIndexIfNeeded();
    }

    private void createIndexIfNeeded() {
        IndicesExistsResponse existsResponse = this.client.admin().indices().prepareExists(INDEX_NAME).execute()
                .actionGet();
        if (!existsResponse.isExists()) {
            //create the index and define dynamic templates field mappings
            CreateIndexRequestBuilder cirb = this.client.admin().indices().prepareCreate(INDEX_NAME)
                    .addMapping("_default_", getIndexDynamicTemplateFieldMapping());
            CreateIndexResponse createIndexResponse = cirb.execute().actionGet();

            //update specific field mappings
            PutMappingRequestBuilder pmrb = this.client.admin().indices().preparePutMapping(INDEX_NAME)
                    .setType(DOCUMENT_TYPE_BACKUP);
            pmrb.setSource(this.getIndexCustomFieldMapping());
            PutMappingResponse putMappingResponse = pmrb.execute().actionGet();
            if ((!createIndexResponse.isAcknowledged()) || (!putMappingResponse.isAcknowledged())) {
                // throw new Exception("Could not create index ["+ INDEX_NAME +"].");
                this.logger.error("Could not create index [" + INDEX_NAME + " ].");
            } else {
                this.logger.debug("Successfully created index [" + INDEX_NAME + " ].");
            }
        }
        //backward compatibility: check if we have an outdated version of the index - if so delete and recreate with mapping
        else if (!checkIndexFieldMappingsOK()) {
            this.client.admin().indices().prepareDelete(INDEX_NAME).execute().actionGet();
            this.logger.debug("Deleted [" + INDEX_NAME + " ]"
                    + "due to missing dynamic template or field mapping configuration");
            createIndexIfNeeded();
        }
    }

    private boolean checkIndexFieldMappingsOK() {
        if ((checkIsIndexDynamicTemplateMappingSet()) && (checkIsIndexFieldMappingSet())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the currently existing version of the index was configured properly e.g. if the dynamic template
     * property mapping was set etc
     */
    private boolean checkIsIndexDynamicTemplateMappingSet() {
        GetMappingsResponse mapping = this.client.admin().indices().prepareGetMappings(INDEX_NAME).get();
        try {
            HashMap props = (HashMap) mapping.getMappings().get(INDEX_NAME).get("_default_").getSourceAsMap();
            if (props != null) {
                if (props.containsKey("dynamic_templates")) {
                    ArrayList al = ((ArrayList) props.get("dynamic_templates"));
                    if (al.size() > 0) {
                        HashMap hm = (HashMap) al.get(0);
                        if (hm.containsKey("tikaprops")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            this.logger.debug("Index does not contain dynamic_templates config to set Tika field types to String");
        }
        return false;
    }

    /**
     * Checks if the currently existing version of the index was configured properly e.g. if the indexrecord_uuid field
     * mapping was set etc
     * 
     * check if the field mapping for indexrecord_uuid was set to not analyzed as this causes issues using the terms
     * filter on UUID Strings https://www.elastic.co/guide/en/elasticsearch/guide/current/_finding_exact_values.html
     * https://www.elastic.co/guide/en/elasticsearch/guide/current/analysis-intro.html#analyze-api
     * https://www.elastic.co/guide/en/elasticsearch/guide/current/_finding_exact_values.html
     */
    private boolean checkIsIndexFieldMappingSet() {
        GetMappingsResponse mapping = this.client.admin().indices().prepareGetMappings(INDEX_NAME).get();
        try {
            HashMap props = (HashMap) mapping.getMappings().get(INDEX_NAME).get(DOCUMENT_TYPE_BACKUP).getSourceAsMap();
            if (props != null) {
                if (props.containsKey("properties")) {
                    HashMap fieldMappings = ((HashMap) props.get("properties"));
                    //check if the field mapping for indexrecord_uuid was set to not analyzed 
                    if (fieldMappings.containsKey("indexrecord_uuid")) {
                        HashMap fieldIndexRecordMapping = (HashMap) fieldMappings.get("indexrecord_uuid");
                        if (fieldIndexRecordMapping.containsKey("index")
                                && fieldIndexRecordMapping.get("index").toString().equals("not_analyzed")) {
                            return true;
                        }
                    }

                }
            }
        } catch (Exception e) {
            this.logger.debug("Index does not contain a 'not_analyzed' field mapping for indexrecord_uuid");
        }
        return false;
    }

    /**
     * An alternative approach to disabling date detection and explicitly mapping specific fields as dates is instruct
     * ElasticSearchs dynamic mapping functionality to adhere to naming conventions for fields Mapping sets field type
     * to String for all fields that start with tikaprop_
     * 
     * @see mapping date fields using naming conventions
     *      http://joelabrahamsson.com/dynamic-mappings-and-dates-in-elasticsearch/
     * @return
     */
    private String getIndexDynamicTemplateFieldMapping() {
        return loadJson("elasticsearch_dynamic_templates_config.json");
    }

    /**
     * Defines field mappings for the backmeup/backup which are required other than auto generated e.g. sets the field
     * indexrecord_uuid to not_analyzed due to ES UUID String analysis and handling in terms filters
     * 
     * @return
     */
    private String getIndexCustomFieldMapping() {
        return loadJson("elasticsearch_field_mapping_config.json");
    }

    @Override
    public SearchResultAccumulator queryBackup(String query, String source, String type, String job, String owner,
            String tag, String username, Long offSetStart, Long maxResults) {
        Map<String, List<String>> filters = createFiltersFor(source, type, job, owner, tag);
        return queryBackup(query, filters, username, offSetStart, maxResults);
    }

    private Map<String, List<String>> createFiltersFor(String source, String type, String job, String owner, String tag) {
        Map<String, List<String>> filters = null;

        if (source != null || type != null || job != null || owner != null || tag != null) {
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

        if (owner != null) {
            List<String> filtervalue = new LinkedList<>();
            filtervalue.add(owner);
            filters.put("owner", filtervalue);
        }

        if (tag != null) {
            List<String> filtervalue = new LinkedList<>();
            filtervalue.add(tag);
            filters.put("tag", filtervalue);
        }

        return filters;
    }

    public SearchResultAccumulator queryBackup(String query, Map<String, List<String>> filters, String username,
            Long offSetStart, Long maxResults) {
        //size Indicates the number of results that should be returned, defaults to 10 
        if (maxResults == null || maxResults < 0) {
            maxResults = Long.valueOf(100);
        }
        //from Indicates the number of initial results that should be skipped, defaults to 0 
        if (offSetStart == null || offSetStart < 0) {
            offSetStart = Long.valueOf(0);
        }

        SearchResponse esResponse = queryBackup(query, filters, offSetStart, maxResults);
        SearchResultAccumulator result = new SearchResultAccumulator();
        result.setFiles(IndexUtils.convertSearchEntries(esResponse, username));
        result.setBySource(IndexUtils.getBySource(esResponse));
        result.setByType(IndexUtils.getByType(esResponse));
        result.setByJob(IndexUtils.getByJob(esResponse));
        result.setByOwner(IndexUtils.getByOwner(esResponse));
        //set the offset and nr of elements requested for this search
        result.setOffsetStart(offSetStart);
        result.setOffsetEnd(offSetStart + maxResults);
        //requires Elasticsearch index and database operations to retrieve these objects
        result.setByTag(IndexUtils.getByTag(esResponse, this.taggedCollectionDao));
        return result;
    }

    private SearchResponse queryBackup(String query, Map<String, List<String>> filters, Long offSetStart,
            Long maxElements) {
        String queryString = buildQuery(query);

        /*
         * QueryBuilder qBuilder = QueryBuilders.queryString(queryString);
         */

        QueryBuilder qBuilder = IndexUtils.buildQuery(this.userId, queryString, filters, this.taggedCollectionDao);
        this.logger.debug("#######################################");
        this.logger.debug("QueryString:\n" + qBuilder.toString());
        this.logger.debug("#######################################");

        return this.client.prepareSearch(INDEX_NAME).setQuery(qBuilder)
                .addSort(IndexFields.FIELD_BACKUP_AT, SortOrder.DESC).addHighlightedField(IndexFields.FIELD_FULLTEXT)
                .setSize(maxElements.intValue()).setFrom(offSetStart.intValue()).execute().actionGet();
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
        boolean hasIndex = this.client.admin().indices().exists(new IndicesExistsRequest(INDEX_NAME)).actionGet()
                .isExists();
        if (hasIndex) {
            this.client.prepareDeleteByQuery(INDEX_NAME)
                    .setQuery(QueryBuilders.matchQuery(IndexFields.FIELD_OWNER_ID, this.userId)).execute().actionGet();
        }
    }

    @Override
    public void deleteRecordsForUserAndJobAndTimestamp(Long jobId, Date timestamp) {
        QueryBuilder qBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery(IndexFields.FIELD_JOB_ID, jobId))
                .must(QueryBuilders.matchQuery(IndexFields.FIELD_BACKUP_AT, timestamp.getTime()));

        this.client.prepareDeleteByQuery(INDEX_NAME).setQuery(qBuilder).execute().actionGet();
    }

    @Override
    public void deleteRecordsForUserAndDocumentUUID(UUID documentUUID) {
        QueryBuilder qBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery(IndexFields.FIELD_OWNER_ID, this.userId))
                .must(QueryBuilders.matchQuery(IndexFields.FIELD_INDEX_DOCUMENT_UUID, documentUUID));

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
        try (XContentBuilder elasticBuilder = new ElasticContentBuilder(document).asElastic()) {
            IndexResponse response = this.client.prepareIndex(INDEX_NAME, DOCUMENT_TYPE_BACKUP)
                    .setSource(elasticBuilder).setRefresh(true).execute().actionGet();
            this.logger.debug("ingested in index: " + response.getIndex() + " type: " + response.getType() + " id: "
                    + response.getId());
        }
        this.logger.debug("Done sending IndexDocument to ES");
    }

    /**
     * Load content of a json file to String using classloader and resource as stream
     * 
     * @param fileName
     * @return
     * @throws IOException
     */
    private String loadJson(String fileName) {
        try {
            return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(fileName));
        } catch (IOException e) {
            this.logger.error("was not able to read json file from disk: " + fileName);
            return null;
        }
    }
}
