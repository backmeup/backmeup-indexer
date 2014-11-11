package org.backmeup.index;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.backmeup.data.dummy.ElasticContentBuilder;
import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.data.dummy.ThemisDataSink.IndexFragmentType;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.utils.file.JsonSerializer;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the handle for importing and deleting shared index fragments When a backup is shared with
 * another user IndexDocument is stored in a deposit box encrypted with the users public key which gets then updated
 * once the index is started i.e. the user logs onto the system
 * 
 */
public class IndexSharingHandler {

    private static final Logger log = LoggerFactory.getLogger(IndexSharingHandler.class);

    /**
     * User A grants permission to user B on a specific indexDocument This is copied into user B's public drop off area
     * and gets ingested into elasticsearch when the user logs into the system. The object must have already been stored
     * in the data sink and is references via UUID Shared objects will have the same UUID for two different users
     * 
     * @param fromUserID
     * @param withUserID
     * @param indexFragment
     * @return the UUID of the object for userB
     */
    public static UUID shareIndexFragment(int fromUserID, int withUserID, UUID objectID) throws IOException {
        // TODO
        return null;
    }

    public static void revokeIndexFragmentSharing(int fromUserID, int withUserID, UUID objectID) throws IOException {
        // TODO
    }

    public void importSharedIndexFragmentInES(UUID objectID, int userID) {
        // TODO
    }

    public void removeSharedIndexFragmentFromES(UUID objectID, int userID) {
        // TODO
    }

    public void importAllSharedIndexFragmentsInES(int userID) {

    }

    /**
     * The ES index gets dropped. The IndexDocuments within the user's fragment directory + the shared fragments from
     * other users are taken to rebuild the index from scratch
     * 
     * @param userID
     */
    public static void rebuildESIndexFromFileBasis(int userID) {
        // TODO drop ES index, take all fragments on disk an rebuild it
    }

    /**
     * Imports all user owned index fragments into ES which have not yet been imported into the index
     * 
     * @param userID
     */
    public void importOwnedIndexFragmentInES(int userID) {

        List<UUID> uuids = ThemisDataSink.getAllIndexFragmentUUIDs(userID, IndexFragmentType.TO_IMPORT_USER_OWNED);
        for (UUID uuid : uuids) {
            try {
                // get the file
                IndexDocument doc = ThemisDataSink.getIndexFragment(uuid, userID,
                        IndexFragmentType.TO_IMPORT_USER_OWNED);
                // import it in elasticsearch
                importIndexFragmentInES(doc, userID);
                // create record in imported, delete record in to_import
                ThemisDataSink.saveIndexFragment(doc, userID, IndexFragmentType.IMPORTED_USER_OWNED);
                ThemisDataSink.deleteIndexFragment(uuid, userID, IndexFragmentType.TO_IMPORT_USER_OWNED);
                // TODO keep a ImportedIndexFragment history in the database

            } catch (IOException e) {
                log.error("Failed to fetch or import IndexFragment " + uuid + " of type "
                        + IndexFragmentType.TO_IMPORT_USER_OWNED + " " + e);
                // TODO ADD PROPER EXCEPTION
            }
        }
    }

    private static void importIndexFragmentInES(IndexDocument doc, int userID) {

        try {
            Client client = IndexManager.getInstance().getESTransportClient(userID);
            index(client, doc);

        } catch (IndexManagerCoreException | IOException e) {
            log.error("failed to add IndexDocument " + JsonSerializer.serialize(doc) + " for userID: " + userID + " "
                    + e);
            // TODO ADD PROPER EXCEPTION
        }
    }

    private static IndexResponse index(Client client, IndexDocument document) throws IOException {
        log.debug("Sending IndexDocument to ES index...");
        XContentBuilder elasticBuilder = new ElasticContentBuilder(document).asElastic();
        IndexResponse response = client
                .prepareIndex(ElasticContentBuilder.INDEX_NAME, ElasticContentBuilder.DOCUMENT_TYPE_BACKUP)
                .setSource(elasticBuilder).setRefresh(true).execute().actionGet();

        log.debug("ingested in index: " + response.getIndex() + " type: " + response.getType() + " id: "
                + response.getId());
        log.debug("Done sending IndexDocument to ES");

        return response;
    }

    public void removeOwnedIndexFragmentInES(UUID objectID, int userID) {

    }
}
