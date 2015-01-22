package org.backmeup.index.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.CountedEntry;
import org.backmeup.index.model.FileInfo;
import org.backmeup.index.model.FileItem;
import org.backmeup.index.model.SearchEntry;
import org.backmeup.index.model.User;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IndexUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexUtils.class);

    private static final String THUMBNAILS_FOLDER = "thumbnails/";

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

            if (source.get(IndexFields.FIELD_THUMBNAIL_PATH) != null) {
                //TODO andrew is this correct? FIXME
                fileItem.setThumbnailURL(THUMBNAILS_FOLDER + owner + "/" + fileId);
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
        fi.setSource(source.get(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_NAME) + " ("
                + source.get(IndexFields.FIELD_BACKUP_SOURCE_IDENTIFICATION) + ")");
        fi.setSourceId(source.get(IndexFields.FIELD_BACKUP_SOURCE_ID).toString());
        fi.setTimeStamp(timestamp.longValue());
        fi.setTitle(source.get(IndexFields.FIELD_FILENAME).toString());
        fi.setPath(source.get(IndexFields.FIELD_PATH).toString());
        fi.setSink(source.get(IndexFields.FIELD_BACKUP_SINK).toString());
        Object contentType = source.get(IndexFields.FIELD_CONTENT_TYPE);
        if (contentType != null) {
            fi.setType(getTypeFromMimeType(contentType.toString()));
        } else {
            fi.setType(getTypeFromMimeType("other"));
        }
        return fi;
    }

    public static List<SearchEntry> convertSearchEntries(org.elasticsearch.action.search.SearchResponse esResponse,
            String userName) {
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
                preview = new StringBuilder("... ");
                preview.append(fulltext.replace("\n", " ").trim() + " ... ");
            }
            // END workaround

            String hash = source.get(IndexFields.FIELD_FILE_HASH).toString();
            Integer owner = (Integer) source.get(IndexFields.FIELD_OWNER_ID);
            Long timestamp = (Long) source.get(IndexFields.FIELD_BACKUP_AT);

            SearchEntry entry = new SearchEntry();

            // We're constructing a (reasonably) unique ID using owner, hash and timestamp
            entry.setFileId(owner + ":" + hash + ":" + timestamp);
            entry.setTitle(source.get(IndexFields.FIELD_FILENAME).toString());
            entry.setTimeStamp(new Date(timestamp));

            if (source.get(IndexFields.FIELD_BACKUP_SOURCE_ID) != null) {
                entry.setDatasourceId(source.get(IndexFields.FIELD_BACKUP_SOURCE_ID).toString());
                entry.setDatasource(source.get(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_NAME) + " ("
                        + source.get(IndexFields.FIELD_BACKUP_SOURCE_IDENTIFICATION) + ")");
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
                    //e.g. http://localhost:8080/backmeup-storage-service/files/#REPLACEAUTHTOKEN#/
                    String sinkDownloadBaseURL = (String) source.get(IndexFields.FIELD_SINK_DOWNLOAD_BASE);
                    //e.g. BMU_filegenerator_492_22_01_2015_21_14/folder1/text01.txt
                    String relPathOnSink = entry.getProperty(IndexFields.FIELD_PATH);
                    //TODO andrew check baseURL + relPath with proper slashes / 
                    entry.setProperty("downloadURL", sinkDownloadBaseURL + relPathOnSink);
                }
            }

            entry.copyPropertyIfExist(IndexFields.FIELD_BACKUP_SINK, source);

            entry.setProperty(IndexFields.FIELD_FILE_HASH, hash);

            if (source.get(IndexFields.FIELD_THUMBNAIL_PATH) != null) {
                entry.setThumbnailUrl(THUMBNAILS_FOLDER + userName + "/" + owner + ":" + hash + ":" + timestamp);
            }

            // Custom props for e.g. facebook, mail plugin
            entry.copyPropertyIfExist("destination", source);
            entry.copyPropertyIfExist("message", source);
            entry.copyPropertyIfExist("parent", source);
            entry.copyPropertyIfExist("author", source);
            entry.copyPropertyIfExist("source", source);
            entry.copyPropertyIfExist("likes", source);
            entry.copyPropertyIfExist("tags", source);
            entry.copyPropertyIfExist("modified", source);

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

    private static List<CountedEntry> groupBySource(org.elasticsearch.action.search.SearchResponse esResponse) {
        // Now where's my Scala groupBy!? *heul*
        Map<String, Integer> groupedHits = new HashMap<>();
        for (SearchHit hit : esResponse.getHits()) {
            if (hit.getSource().get(IndexFields.FIELD_JOB_ID) != null) {
                String backupSourcePluginName = hit.getSource().get(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_NAME)
                        .toString();
                String backupSourceIdentification = hit.getSource().get(IndexFields.FIELD_BACKUP_SOURCE_IDENTIFICATION)
                        .toString();
                String label = backupSourcePluginName + " (" + backupSourceIdentification + ")";
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

                filterstr.append("(" + IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_NAME + ":" + source + " AND "
                        + IndexFields.FIELD_BACKUP_SOURCE_IDENTIFICATION + ":" + profile + ") OR ");
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

                filterstr.append("(" + IndexFields.FIELD_BACKUP_AT + ":" + timestamp + " AND "
                        + IndexFields.FIELD_JOB_NAME + ":" + jobname + ") OR ");
            }

            // remove the last " OR " and close the search string for this part
            filterstr.setLength(filterstr.length() - 4);
            filterstr.append(") AND ");
        }

        return filterstr.toString();
    }

    public static QueryBuilder buildQuery(User userid, String queryString, Map<String, List<String>> filters) {
        BoolQueryBuilder qBuilder = new BoolQueryBuilder();
        qBuilder.must(QueryBuilders.matchQuery(IndexFields.FIELD_OWNER_ID, userid));
        qBuilder.must(QueryBuilders.queryString(queryString));

        if (filters == null) {
            return qBuilder;
        }

        BoolQueryBuilder typematches = buildTypeQuery(filters);
        BoolQueryBuilder sourcematches = buildSourceQuery(filters);
        BoolQueryBuilder jobmatches = buildJobQuery(filters);

        if (typematches != null) {
            qBuilder.must(typematches);
        }
        if (sourcematches != null) {
            qBuilder.must(sourcematches);
        }
        if (jobmatches != null) {
            qBuilder.must(jobmatches);
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

            for (String filter : filters.get("source")) {
                // get out the source plugin, result will be
                // "org.backmeup.source"
                String source = filter.substring(0, filter.indexOf(" "));

                // get out the profile "(Profilename)"
                String profile = filter.substring(filter.indexOf(" ") + 1, filter.length());

                // remove the brackets at begin and end, result will be "ProfileName"
                profile = profile.substring(1, profile.length() - 1);

                BoolQueryBuilder tempbuilder = new BoolQueryBuilder();
                tempbuilder.must(QueryBuilders.matchPhraseQuery(IndexFields.FIELD_BACKUP_SOURCE_PLUGIN_NAME, source));
                tempbuilder.must(QueryBuilders
                        .matchPhraseQuery(IndexFields.FIELD_BACKUP_SOURCE_IDENTIFICATION, profile));

                // tempbuilder1 or tempbulder2 or ...
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
}
