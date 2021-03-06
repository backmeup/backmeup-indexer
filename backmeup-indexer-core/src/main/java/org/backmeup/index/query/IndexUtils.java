package org.backmeup.index.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.backmeup.index.api.IndexFields;
import org.backmeup.index.dal.TaggedCollectionDao;
import org.backmeup.index.model.CountedEntry;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.SearchEntry;
import org.backmeup.index.model.User;
import org.backmeup.index.tagging.TaggedCollection;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.MissingFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IndexUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexUtils.class);

    private IndexUtils() {
    }

    public static Set<FileItem> convertToFileItems(org.elasticsearch.action.search.SearchResponse esResponse) {
        Set<FileItem> fItems = new HashSet<>();

        for (SearchHit hit : esResponse.getHits()) {
            FileItem fileItem = new FileItem();

            Map<String, Object> source = hit.getSource();
            String hash = source.get(IndexFields.FIELD_FILE_HASH).toString();
            Integer owner = (Integer) source.get(IndexFields.FIELD_OWNER_ID);
            Long timestamp = (Long) source.get(IndexFields.FIELD_BACKUP_AT);

            String fileId = owner + ":" + hash + ":" + timestamp;
            fileItem.setFileId(fileId);
            fileItem.setTitle(source.get(IndexFields.FIELD_FILENAME).toString());
            fileItem.setTimeStamp(new Date(timestamp));

            //set the thumbnail if available
            if (source.get(IndexFields.FIELD_SINK_DOWNLOAD_BASE) != null) {
                if (source.get(IndexFields.FIELD_THUMBNAIL_PATH) != null) {
                    String sinkDownloadBaseURL = (String) source.get(IndexFields.FIELD_SINK_DOWNLOAD_BASE);
                    fileItem.setThumbnailURL(sinkDownloadBaseURL + "###TOKEN###/" + source.get(IndexFields.FIELD_THUMBNAIL_PATH).toString());
                }
            }

            fItems.add(fileItem);
        }

        return fItems;
    }

    public static FileInfo convertToFileInfo(org.elasticsearch.action.search.SearchResponse esResponse) {
        if (esResponse.getHits().totalHits() == 0) {
            return null;
        }

        SearchHit hit = esResponse.getHits().getHits()[0];
        Map<String, Object> source = hit.getSource();
        String hash = source.get(IndexFields.FIELD_FILE_HASH).toString();
        Integer owner = (Integer) source.get(IndexFields.FIELD_OWNER_ID);
        Long timestamp = (Long) source.get(IndexFields.FIELD_BACKUP_AT);
        FileInfo fi = new FileInfo();
        fi.setFileId(owner + ":" + hash + ":" + timestamp);
        fi.setSource(source.get(IndexFields.FIELD_BACKUP_SOURCE_AUTH_TITLE) + " (" + source.get(IndexFields.FIELD_BACKUP_SOURCE_PROFILE_ID)
                + ")");
        fi.setSourceId(source.get(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_ID).toString());
        fi.setTimeStamp(timestamp.longValue());
        fi.setTitle(source.get(IndexFields.FIELD_FILENAME).toString());
        fi.setPath(source.get(IndexFields.FIELD_PATH).toString());
        Object contentType = source.get(IndexFields.FIELD_CONTENT_TYPE);
        if (contentType != null) {
            fi.setType(getTypeFromMimeType(contentType.toString()));
        } else {
            fi.setType(getTypeFromMimeType("other"));
        }
        return fi;
    }

    public static List<SearchEntry> convertSearchEntries(org.elasticsearch.action.search.SearchResponse esResponse, String userName) {
        List<SearchEntry> entries = new ArrayList<>();

        LOGGER.debug("converting " + esResponse.getHits().totalHits() + " search results");

        for (SearchHit hit : esResponse.getHits()) {
            Map<String, Object> source = hit.getSource();

            StringBuilder preview = null;
            //TODO Andrew need to check why HighlightFields is not working properly. It is set in the query.
            /*HighlightField highlight = hit.getHighlightFields().get(IndexFields.FIELD_FULLTEXT);
            if (highlight != null) {
                preview = new StringBuilder("... ");
                for (Text fragment : highlight.fragments()) {
                    preview.append(fragment.string().replace("\n", " ").trim() + " ... ");
                }
            }*/

            //TODO workaround for now
            if (source.get(IndexFields.FIELD_FULLTEXT) != null) {
                String fulltext = source.get(IndexFields.FIELD_FULLTEXT).toString();
                //return 250 chars from the fulltext entry within the search results
                preview = createPreview(fulltext);
            }
            // END workaround

            String hash = source.get(IndexFields.FIELD_FILE_HASH).toString();
            Integer owner = (Integer) source.get(IndexFields.FIELD_OWNER_ID);
            Long timestamp = (Long) source.get(IndexFields.FIELD_BACKUP_AT);

            SearchEntry entry = new SearchEntry();

            // We're using the document UUID which is required for sharing
            entry.setFileId(source.get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());
            entry.setTitle(source.get(IndexFields.FIELD_FILENAME).toString());
            entry.setTimeStamp(new Date(timestamp));
            if (source.get(IndexFields.FIELD_SHARED_BY_USER_ID) == null) {
                //we only set the shared_by field in elements that weren't produced by the current user
                entry.setOwnerId(source.get(IndexFields.FIELD_OWNER_ID).toString());
                entry.setIsSharing(false);
            } else {
                entry.setOwnerId(source.get(IndexFields.FIELD_SHARED_BY_USER_ID).toString());
                entry.setIsSharing(true);
            }
            if (source.get(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_ID) != null) {
                String sID = source.get(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_ID).toString();
                entry.setDatasourceId(sID);
                entry.setDatasource(sID);
                if (source.get(IndexFields.FIELD_BACKUP_SOURCE_AUTH_TITLE) != null) {
                    //e.g. org.backmeup.facebook (Wolfgang Eibner)"
                    entry.setDatasource(sID + " (" + source.get(IndexFields.FIELD_BACKUP_SOURCE_AUTH_TITLE).toString() + ")");
                }
            }

            if (source.get(IndexFields.FIELD_BACKUP_SINK_PLUGIN_ID) != null) {
                String sID = source.get(IndexFields.FIELD_BACKUP_SINK_PLUGIN_ID).toString();
                entry.setDatasinkId(sID);
                entry.setDatasink(sID);
                if (source.get(IndexFields.FIELD_BACKUP_SINK_AUTH_TITLE) != null) {
                    entry.setDatasink(sID + " (" + source.get(IndexFields.FIELD_BACKUP_SINK_AUTH_TITLE).toString() + ")");
                }
            }

            if (source.get(IndexFields.FIELD_JOB_NAME) != null) {
                entry.setJobName(source.get(IndexFields.FIELD_JOB_NAME).toString());
            }

            if (preview != null) {
                entry.setPreview(preview.toString().trim());
            }

            Object contentType = source.get(IndexFields.FIELD_CONTENT_TYPE);
            if (contentType != null) {
                entry.setType(getTypeFromMimeType(contentType.toString()));
            } else {
                entry.setType("other");
            }

            entry.setProperty(IndexFields.FIELD_PATH, source.get(IndexFields.FIELD_PATH).toString());

            //add the download URL if sink supports always on
            if (source.get(IndexFields.FIELD_SINK_DOWNLOAD_BASE) != null) {
                if (entry.getProperty(IndexFields.FIELD_PATH) != null) {
                    //e.g. http://localhost:8080/backmeup-storage-service/download/
                    String sinkDownloadBaseURL = (String) source.get(IndexFields.FIELD_SINK_DOWNLOAD_BASE);
                    //e.g. BMU_filegenerator_492_22_01_2015_21_14/folder1/text01.txt
                    String relPathOnSink = entry.getProperty(IndexFields.FIELD_PATH);
                    entry.setDownloadUrl(sinkDownloadBaseURL + "###TOKEN###/" + relPathOnSink);

                    //check if a thumbnail is available and attach
                    if (source.get(IndexFields.FIELD_THUMBNAIL_PATH) != null) {
                        entry.setThumbnailUrl(sinkDownloadBaseURL + "###TOKEN###/"
                                + source.get(IndexFields.FIELD_THUMBNAIL_PATH).toString());
                    }
                }
            }

            //get Tika Metadata fields from ES and add to Metadata properties 
            entry.copyTikaMetadataIfExist(source);
            //get the standardized Geo and Temporal Metadata properties
            entry.copyStandardizedGeoAndTemporalMetadataIfExist(source);
            //add md5 hash
            entry.setMetadata(IndexFields.FIELD_FILE_HASH, hash);

            // Custom props for e.g. facebook, mail plugin
            /*entry.copyPropertyIfExist("destination", source);
            entry.copyPropertyIfExist("message", source);
            entry.copyPropertyIfExist("parent", source);
            entry.copyPropertyIfExist("author", source);
            entry.copyPropertyIfExist("source", source);
            entry.copyPropertyIfExist("likes", source);
            entry.copyPropertyIfExist("tags", source);
            entry.copyPropertyIfExist("modified", source);*/

            entries.add(entry);
        }
        return entries;
    }

    public static List<CountedEntry> getBySource(org.elasticsearch.action.search.SearchResponse esResponse) {
        // TODO we currently group by 'list of sources' rather than source
        return groupBySource(esResponse);
    }

    public static List<CountedEntry> getByType(org.elasticsearch.action.search.SearchResponse esResponse) {
        return groupByContentType(esResponse);
    }

    public static List<CountedEntry> getByJob(org.elasticsearch.action.search.SearchResponse esResponse) {
        return groupByContentJob(esResponse);
    }

    public static List<CountedEntry> getByOwner(org.elasticsearch.action.search.SearchResponse esResponse) {
        return groupByContentOwner(esResponse);
    }

    public static List<CountedEntry> getByTag(org.elasticsearch.action.search.SearchResponse esResponse, TaggedCollectionDao dao) {
        return groupByTaggedCollection(esResponse, dao);
    }

    private static List<CountedEntry> groupBySource(org.elasticsearch.action.search.SearchResponse esResponse) {
        // Now where's my Scala groupBy!? *heul*
        Map<String, Integer> groupedHits = new HashMap<>();
        for (SearchHit hit : esResponse.getHits()) {
            if (hit.getSource().get(IndexFields.FIELD_JOB_ID) != null) {
                String backupSourceAuthTitle = "";
                if (hit.getSource().get(IndexFields.FIELD_BACKUP_SOURCE_AUTH_TITLE) != null) {
                    backupSourceAuthTitle = " (" + hit.getSource().get(IndexFields.FIELD_BACKUP_SOURCE_AUTH_TITLE).toString() + ")";
                }
                String backupSourcePluginID = hit.getSource().get(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_ID).toString();
                //e.g. org.backmeup.facebook (Wolfgang Eibner)"
                String label = backupSourcePluginID + backupSourceAuthTitle;
                Integer count = groupedHits.get(label);
                if (count == null) {
                    count = Integer.valueOf(1);
                } else {
                    count = Integer.valueOf(count.intValue() + 1);
                }
                groupedHits.put(label, count);
            }
        }

        // ...and .map
        List<CountedEntry> countedEntries = new ArrayList<>();
        for (Entry<String, Integer> entry : groupedHits.entrySet()) {
            countedEntries.add(new CountedEntry(entry.getKey(), entry.getValue().intValue()));
        }

        return countedEntries;
    }

    private static List<CountedEntry> groupByContentType(org.elasticsearch.action.search.SearchResponse esResponse) {
        // Now where's my Scala groupBy!? *heul*
        Map<String, Integer> groupedHits = new HashMap<>();
        for (SearchHit hit : esResponse.getHits()) {
            String type;
            if (hit.getSource().get(IndexFields.FIELD_CONTENT_TYPE) != null) {
                type = getTypeFromMimeType(hit.getSource().get(IndexFields.FIELD_CONTENT_TYPE).toString());
            } else {
                type = "other";
            }

            Integer count = groupedHits.get(type);
            if (count == null) {
                count = Integer.valueOf(1);
            } else {
                count = Integer.valueOf(count.intValue() + 1);
            }
            groupedHits.put(type, count);
        }

        // ...and .map
        List<CountedEntry> countedEntries = new ArrayList<>();
        for (Entry<String, Integer> entry : groupedHits.entrySet()) {
            countedEntries.add(new CountedEntry(entry.getKey(), entry.getValue().intValue()));
        }

        return countedEntries;
    }

    private static List<CountedEntry> groupByContentJob(org.elasticsearch.action.search.SearchResponse esResponse) {
        // Now where's my Scala groupBy!? *heul*
        Map<String, Integer> groupedHits = new HashMap<>();
        for (SearchHit hit : esResponse.getHits()) {
            if (hit.getSource().get(IndexFields.FIELD_JOB_ID) != null) {
                String backupJobName = hit.getSource().get(IndexFields.FIELD_JOB_NAME).toString();
                String backupTimestamp = hit.getSource().get(IndexFields.FIELD_BACKUP_AT).toString();
                String label = backupJobName + " (" + backupTimestamp + ")";
                Integer count = groupedHits.get(label);
                if (count == null) {
                    count = Integer.valueOf(1);
                } else {
                    count = Integer.valueOf(count.intValue() + 1);
                }
                groupedHits.put(label, count);
            }
        }

        // ...and .map
        List<CountedEntry> countedEntries = new ArrayList<>();
        for (Entry<String, Integer> entry : groupedHits.entrySet()) {
            countedEntries.add(new CountedEntry(entry.getKey(), entry.getValue().intValue()));
        }

        return countedEntries;
    }

    private static List<CountedEntry> groupByContentOwner(org.elasticsearch.action.search.SearchResponse esResponse) {
        Map<String, Integer> groupedHits = new HashMap<>();
        for (SearchHit hit : esResponse.getHits()) {
            if (hit.getSource().get(IndexFields.FIELD_JOB_ID) != null) {
                String backupByUserId, label;
                if (hit.getSource().get(IndexFields.FIELD_SHARED_BY_USER_ID) == null) {
                    //in this case its a record that's not shared but user owns himself
                    backupByUserId = hit.getSource().get(IndexFields.FIELD_OWNER_ID).toString();
                    String ownerName = hit.getSource().get(IndexFields.FIELD_OWNER_NAME).toString();
                    label = "my data (" + ownerName + "): " + backupByUserId;
                } else {
                    //this record is one that's shared by another user
                    backupByUserId = hit.getSource().get(IndexFields.FIELD_SHARED_BY_USER_ID).toString();
                    label = "shared data user: " + backupByUserId;
                }

                Integer count = groupedHits.get(label);
                if (count == null) {
                    count = Integer.valueOf(1);
                } else {
                    count = Integer.valueOf(count.intValue() + 1);
                }
                groupedHits.put(label, count);
            }
        }
        // ...and .map
        List<CountedEntry> countedEntries = new ArrayList<>();
        for (Entry<String, Integer> entry : groupedHits.entrySet()) {
            countedEntries.add(new CountedEntry(entry.getKey(), entry.getValue().intValue()));
        }
        return countedEntries;
    }

    private static List<CountedEntry> groupByTaggedCollection(org.elasticsearch.action.search.SearchResponse esResponse,
            TaggedCollectionDao dao) {

        Map<String, Integer> groupedHits = new HashMap<>();
        for (SearchHit hit : esResponse.getHits()) {
            if (hit.getSource().get(IndexFields.FIELD_JOB_ID) != null) {
                String documentUUID, userId;
                documentUUID = hit.getSource().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString();
                userId = hit.getSource().get(IndexFields.FIELD_OWNER_ID).toString();
                //fetch the names and collection id of the tagged collections where this document is contained in
                List<TaggedCollection> elementInCollections = dao.getAllActiveFromUserContainingDocumentIds(new User(Long.valueOf(userId)),
                        new ArrayList<UUID>(Arrays.asList(UUID.fromString(documentUUID))));
                for (TaggedCollection element : elementInCollections) {
                    String label = element.getName() + " (" + element.getId() + ")";

                    Integer count = groupedHits.get(label);
                    if (count == null) {
                        count = Integer.valueOf(1);
                    } else {
                        count = Integer.valueOf(count.intValue() + 1);
                    }

                    groupedHits.put(label, count);
                }
            }
        }
        // ...and .map
        List<CountedEntry> countedEntries = new ArrayList<>();
        for (Entry<String, Integer> entry : groupedHits.entrySet()) {
            countedEntries.add(new CountedEntry(entry.getKey(), entry.getValue().intValue()));
        }
        return countedEntries;
    }

    private static String getTypeFromMimeType(String mime) {
        return getTypeFromMimeTypeLowerCase(mime.toLowerCase());
    }

    private static String getTypeFromMimeTypeLowerCase(String mime) {
        if (mime.contains("html")) {
            return "html";
        }
        if (mime.startsWith("image")) {
            return "image";
        }
        if (mime.startsWith("video")) {
            return "video";
        }
        if (mime.startsWith("audio")) {
            return "audio";
        }
        if (mime.startsWith("text")) {
            return "text";
        }
        if (mime.contains("pdf")) {
            return "text";
        }
        if (mime.contains("ogg")) {
            return "audio";
        }

        // Add more special rules as needed 
        return "other";
    }

    public static String getFilterSuffix(Map<String, List<String>> filters) {
        if (filters == null) {
            return "";
        }

        StringBuilder filterstr = new StringBuilder();

        if (filters.containsKey("type")) {
            filterstr.append('(');

            for (String filter : filters.get("type")) {
                if (filter.toLowerCase().equals("html")) {
                    filterstr.append("Content-Type:*html* OR ");
                } else if (filter.toLowerCase().equals("image")) {
                    filterstr.append("Content-Type:image* OR ");
                } else if (filter.toLowerCase().equals("video")) {
                    filterstr.append("Content-Type:video* OR ");
                } else if (filter.toLowerCase().equals("audio")) {
                    filterstr.append("Content-Type:audio* OR ");
                } else if (filter.toLowerCase().equals("text")) {
                    filterstr.append("Content-Type:text* OR ");
                }
            }

            // remove the last " OR " and close the search string for this part
            filterstr.setLength(filterstr.length() - 4);
            filterstr.append(") AND ");
        }

        // TODO if "ProfileName" includes special chars like (,", ... we will have a problem with the search?
        if (filters.containsKey("source")) {
            filterstr.append('(');

            // something like this will come "org.backmeup.source (ProfileName)"
            for (String filter : filters.get("source")) {
                // get out the source plugin, result will be
                // "org.backmeup.source"
                String source = filter.substring(0, filter.indexOf(" "));

                // get out the profile "(Profilename)"
                String profile = filter.substring(filter.indexOf(" ") + 1, filter.length());
                // remove the brackets at begin and end, result will be
                // "ProfileName"
                profile = profile.substring(1, profile.length() - 1);

                filterstr.append("(" + IndexFields.FIELD_BACKUP_SOURCE_AUTH_TITLE + ":" + source + " AND "
                        + IndexFields.FIELD_BACKUP_SOURCE_PROFILE_ID + ":" + profile + ") OR ");
            }

            // remove the last " OR " and close the search string for this part
            filterstr.setLength(filterstr.length() - 4);
            filterstr.append(") AND ");
        }

        // TODO if job contains special chars ...
        if (filters.containsKey("job")) {
            filterstr.append('(');

            // something like this will come "JobName (Timestamp)" (java
            // timestamp -> 13 chars)
            for (String filter : filters.get("job")) {
                // get out the timestamp (also remove the "()").
                String timestamp = filter.substring(filter.length() - 14, filter.length() - 1);

                // get out the job name
                String jobname = filter.substring(0, filter.length() - 16);

                filterstr.append("(" + IndexFields.FIELD_BACKUP_AT + ":" + timestamp + " AND " + IndexFields.FIELD_JOB_NAME + ":" + jobname
                        + ") OR ");
            }

            // remove the last " OR " and close the search string for this part
            filterstr.setLength(filterstr.length() - 4);
            filterstr.append(") AND ");
        }

        return filterstr.toString();
    }

    public static QueryBuilder buildQuery(User userid, String queryString, Map<String, List<String>> filters,
            TaggedCollectionDao taggedCollectionDao) {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        qBuilder.must(QueryBuilders.matchQuery(IndexFields.FIELD_OWNER_ID, userid));
        qBuilder.must(QueryBuilders.queryString(queryString));

        if (filters == null) {
            return qBuilder;
        }

        BoolQueryBuilder typematches = buildTypeQuery(filters);
        BoolQueryBuilder sourcematches = buildSourceQuery(filters);
        BoolQueryBuilder jobmatches = buildJobQuery(filters);
        BoolQueryBuilder ownermatches = buildOwnerQuery(filters);
        BoolQueryBuilder tagmatches = buildTagQuery(filters, taggedCollectionDao);

        if (typematches != null) {
            qBuilder.must(typematches);
        }
        if (sourcematches != null) {
            qBuilder.must(sourcematches);
        }
        if (jobmatches != null) {
            qBuilder.must(jobmatches);
        }
        if (ownermatches != null) {
            qBuilder.must(ownermatches);
        }
        if (tagmatches != null) {
            qBuilder.must(tagmatches);
        }
        return qBuilder;
    }

    private static BoolQueryBuilder buildTypeQuery(Map<String, List<String>> filters) {
        BoolQueryBuilder typematches = null;

        if (filters.containsKey("type")) {
            typematches = new BoolQueryBuilder();
            // minimum 1 of the should clausels must match
            typematches.minimumNumberShouldMatch(1);

            for (String filter : filters.get("type")) {
                if (filter.toLowerCase().equals("html")) {
                    typematches.should(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_CONTENT_TYPE, "*html*"));
                } else if (filter.toLowerCase().equals("image")) {
                    typematches.should(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_CONTENT_TYPE, "image*"));
                } else if (filter.toLowerCase().equals("video")) {
                    typematches.should(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_CONTENT_TYPE, "video*"));
                } else if (filter.toLowerCase().equals("audio")) {
                    typematches.should(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_CONTENT_TYPE, "audio*"));
                } else if (filter.toLowerCase().equals("text")) {
                    typematches.should(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_CONTENT_TYPE, "text*"));
                }
            }
        }

        return typematches;
    }

    private static BoolQueryBuilder buildSourceQuery(Map<String, List<String>> filters) {
        BoolQueryBuilder sourcematches = null;

        if (filters.containsKey("source")) {
            sourcematches = new BoolQueryBuilder();
            // minimum 1 of the should clausels must match
            sourcematches.minimumNumberShouldMatch(1);

            // get out the source plugin, result will be
            for (String filter : filters.get("source")) {
                BoolQueryBuilder tempbuilder = new BoolQueryBuilder();

                //test if we have a profile name set as e.g. "org.backmeup.source (ProfileName)"
                if ((filter.lastIndexOf(" ") != -1) && (filter.lastIndexOf(")") != -1)) {
                    // "org.backmeup.source"
                    String source = filter.substring(0, filter.indexOf(" "));

                    // get out the profile "(Profilename)"
                    String profile = filter.substring(filter.indexOf(" ") + 1, filter.length());

                    // remove the brackets at begin and end, result will be "ProfileName"
                    profile = profile.substring(1, profile.length() - 1);

                    tempbuilder.must(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_ID, source));
                    tempbuilder.must(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_BACKUP_SOURCE_AUTH_TITLE, profile));
                } else {
                    //we don't have a profile set
                    String source = filter;
                    tempbuilder.must(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_ID, source));
                }

                // tempbuilder1 or tempbuilder2 or ...
                sourcematches.should(tempbuilder);
            }
        }

        return sourcematches;
    }

    private static BoolQueryBuilder buildJobQuery(Map<String, List<String>> filters) {
        BoolQueryBuilder jobmatches = null;

        if (filters.containsKey("job")) {
            jobmatches = new BoolQueryBuilder();
            // minimum 1 of the should clausels must match
            jobmatches.minimumNumberShouldMatch(1);

            // something like this will come "JobName (Timestamp)" (java timestamp -> 13 chars)
            for (String filter : filters.get("job")) {
                // get out the timestamp (also remove the "()").
                String timestamp = filter.substring(filter.length() - 14, filter.length() - 1);

                // get out the job name
                String jobname = filter.substring(0, filter.length() - 16);

                BoolQueryBuilder tempbuilder = new BoolQueryBuilder();
                tempbuilder.must(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_BACKUP_AT, timestamp));
                tempbuilder.must(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_JOB_NAME, jobname));

                // tempbuilder1 or tempbulder2 or ...
                jobmatches.should(tempbuilder);
            }
        }

        return jobmatches;
    }

    private static BoolQueryBuilder buildOwnerQuery(Map<String, List<String>> filters) {
        BoolQueryBuilder jobmatches = null;

        if (filters.containsKey("owner")) {
            jobmatches = new BoolQueryBuilder();
            // minimum 1 of the should conditions must match
            jobmatches.minimumNumberShouldMatch(1);

            // something like this will come either "my data (" + ownerName+"): "+ userID or "shared data user: " + userID
            for (String filter : filters.get("owner")) {
                BoolQueryBuilder tempbuilder = new BoolQueryBuilder();

                // get out the owner id
                String dataOwnerId = filter.substring(filter.indexOf(":") + 2, filter.length());

                // decide either own data or shared data which has different Fields within the ES index
                if (filter.startsWith("my data")) {
                    //it's a user owned data element
                    tempbuilder.must(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_OWNER_ID, dataOwnerId));
                    //as owner is set for all entities within the index we need to check that shared_by is not set
                    MissingFilterBuilder noFieldSharedBy = FilterBuilders.missingFilter(IndexFields.FIELD_SHARED_BY_USER_ID);
                    tempbuilder.must(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), noFieldSharedBy));
                } else {
                    //it's a shared data element
                    tempbuilder.must(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_SHARED_BY_USER_ID, dataOwnerId));
                }

                // tempbuilder1 or tempbulder2 or ...
                jobmatches.should(tempbuilder);
            }
        }

        return jobmatches;
    }

    private static BoolQueryBuilder buildTagQuery(Map<String, List<String>> filters, TaggedCollectionDao dao) {
        BoolQueryBuilder tagmatches = null;

        if (filters.containsKey("tag")) {
            tagmatches = new BoolQueryBuilder();
            // minimum 1 of the should conditions must match
            tagmatches.minimumNumberShouldMatch(1);

            // something like this will come "tagged collection name (+"collectionID+")
            for (String filter : filters.get("tag")) {
                BoolQueryBuilder tempbuilder = new BoolQueryBuilder();

                // extract the collection id
                long collectionId = Long.valueOf(filter.substring(filter.lastIndexOf("(") + 1, filter.lastIndexOf(")")));
                TaggedCollection collection = dao.getByEntityId(collectionId);
                List<UUID> documentsInCollection = collection.getDocumentIds();

                //look for docUUIDs that are contained in the tagged collection.
                TermsFilterBuilder docIDsInListofDocs = FilterBuilders.termsFilter(IndexFields.FIELD_INDEX_DOCUMENT_UUID,
                        documentsInCollection);

                tempbuilder.must(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), docIDsInListofDocs));

                // tempbuilder1 or tempbulder2 or ...
                tagmatches.should(tempbuilder);
            }
        }
        return tagmatches;
    }

    private static StringBuilder createPreview(String s) {
        s = s.replace("\n", " ").trim();
        if (s.length() > 250) {
            s = s.substring(0, 250);
        }
        StringBuilder sb = new StringBuilder("... ");
        sb.append(s + " ...");
        return sb;
    }
}
