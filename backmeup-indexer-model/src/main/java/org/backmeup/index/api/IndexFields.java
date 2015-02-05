package org.backmeup.index.api;

/**
 * Symbolic names of content fields of the data to be indexed. All fields are either String or Long.
 */
public class IndexFields {

    public static final String FIELD_OWNER_ID = "owner_id";
    public static final String FIELD_OWNER_NAME = "owner_name";
    public static final String FIELD_FILENAME = "filename";
    public static final String FIELD_PATH = "path";
    public static final String FIELD_THUMBNAIL_PATH = "thumbnail_path";
    public static final String FIELD_BACKUP_SOURCE_ID = "backup_source_id";
    public static final String FIELD_BACKUP_SOURCE_IDENTIFICATION = "backup_source_identification";
    public static final String FIELD_BACKUP_SOURCE_PLUGIN_NAME = "backup_source_plugin_name";
    public static final String FIELD_BACKUP_SINK_ID = "backup_sink_id";
    public static final String FIELD_BACKUP_SINK_IDENTIFICATION = "backup_sink_identification";
    public static final String FIELD_BACKUP_SINK_PLUGIN_NAME = "backup_sink_plugin_name";
    public static final String FIELD_FILE_HASH = "file_md5_hash";
    public static final String FIELD_BACKUP_AT = "backup_at";
    public static final String FIELD_CONTENT_TYPE = "Content-Type";
    public static final String FIELD_JOB_ID = "job_id";
    public static final String FIELD_JOB_NAME = "job_name";
    public static final String FIELD_FULLTEXT = "fulltext";
    public static final String FIELD_SINK_DOWNLOAD_BASE = "sink_download_base";

    /*----Fields manipulated by the Index-Core or Storage components----*/
    public static final String FIELD_INDEX_DOCUMENT_UUID = "indexrecord_uuid";

}
