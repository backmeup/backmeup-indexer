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
    public static final String FIELD_BACKUP_SOURCE_PLUGIN_ID = "backup_source_plugin_id";
    public static final String FIELD_BACKUP_SOURCE_PROFILE_ID = "backup_source_profile_id";
    public static final String FIELD_BACKUP_SOURCE_AUTH_TITLE = "backup_source_auth_title";
    public static final String FIELD_BACKUP_SINK_PLUGIN_ID = "backup_sink_plugin_id";
    public static final String FIELD_BACKUP_SINK_PROFILE_ID = "backup_sink_profile_id";
    public static final String FIELD_BACKUP_SINK_AUTH_TITLE = "backup_sink_auth_title";
    public static final String FIELD_FILE_HASH = "file_md5_hash";
    public static final String FIELD_BACKUP_AT = "backup_at";
    public static final String FIELD_CONTENT_TYPE = "Content-Type";
    public static final String FIELD_JOB_ID = "job_id";
    public static final String FIELD_JOB_NAME = "job_name";
    public static final String FIELD_FULLTEXT = "fulltext";
    public static final String FIELD_SINK_DOWNLOAD_BASE = "sink_download_base";

    /*----Fields manipulated by the Index-Core or Storage components----*/
    public static final String FIELD_INDEX_DOCUMENT_UUID = "indexrecord_uuid";

    /*----Fields which are provided by Tika and are used to build up the ojbect's metadata-----*/
    //currently for doxc and pdf
    public enum TikaMetadataFields {
        DC_SUBJECT("dc:subject"), DC_CREATOR("dc:creator"), DC_TITLE("dc:title"), DCTERMS_MODIFIED("dcterms:modified"), DCTERMS_CREATED(
                "dcterms:created"), META_CREATION_DATE("meta:creation-date"), META_SAVE_DATE("meta:save-date"), META_AUTHOR(
                "meta:author"), META_KEYWORD("meta:keyword"), XMPTPG_NPAGES("xmpTPg:NPages"), XMP_CREATOR_TOOL(
                "xmp:CreatorTool"), PRODUCER("producer"), LAST_MODIFIED("Last-Modified"), LAST_SAVE_DATE(
                "Last-Save-Date"), CREATION_DATE("Creation-Date"), META_LAST_AUTHOR("meta:last-author"), APPLICATION_NAME(
                "Application-Name"), APPLICATION_VERSION("Application-Version"), CHARACTER_COUNT_WITH_SPACES(
                "Character-Count-With-Spaces"), EXTENDED_PROPERTIES_TEMPLATE("extended-properties:Template"), META_LINE_COUNT(
                "meta:line-count"), PUBLISHER("publisher"), WORD_COUNT("Word-Count"), META_PARAGRAPH_COUNT(
                "meta:paragraph-count"), EXTENDED_PROPERTIES_APPVERSION("extended-properties:AppVersion"), LINE_COUNT(
                "Line-Count"), EXTENDED_PROPERTIES_APPLICATION("extended-properties:Application"), PARAGRAPH_COUNT(
                "Paragraph-Count"), REVISION_NUMBER("Revision-Number"), PAGE_COUNT("Page-Count"), META_CHARACTER_COUNT(
                "meta:character-count"), META_WORD_COUNT("meta:word-count"), EXTENDED_PROPERTIES_COMPANY(
                "extended-properties:Company"), X_PARSED_BY("X-Parsed-By"), DC_PUBLISHER("dc:publisher"), META_PAGE_COUNT(
                "meta:page-count"), META_CHARACTER_COUNT_WITH_SPACES("meta:character-count-with-spaces"), CONTENT_TYPE(
                "Content-Type");

        private String key;

        private TikaMetadataFields(String key) {
            this.key = key;
        }

        public String getFieldKey() {
            return this.key;
        }
    }

}
