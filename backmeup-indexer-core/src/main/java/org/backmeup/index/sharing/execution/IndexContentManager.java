package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.backmeup.index.IndexManager;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.elasticsearch.SearchInstanceException;
import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ElasticSearchSetup;
import org.backmeup.index.serializer.Json;
import org.backmeup.index.storage.ThemisDataSink;
import org.backmeup.index.storage.ThemisEncryptedPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the handle for importing and deleting shared index fragments When a backup is shared with
 * another user IndexDocument is stored in a deposit box encrypted with the users public key which gets then updated
 * once the index is started i.e. the user logs onto the system
 */
@ApplicationScoped
public class IndexContentManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexManager indexManager;
    @Inject
    private IndexFragmentEntryStatusDao entryStatusDao;
    @Inject
    private RunningIndexUserConfigDao runninInstancesDao;
    @Inject
    private ElasticSearchSetup esSetup;

    /**
     * The ES index gets dropped. Iterate over all import, deletion operations of IndexDocuments (shared and user owned)
     * from the DB (IndexFragmentEntryStatus) and rebuild the The IndexDocuments within the user's fragment directory +
     * the shared fragments from ES Index from scratch.
     */
    public static void rebuildESIndexFromScratch(User user) {
        // TODO AL drop ES index, take all fragments on disk an rebuild it
    }

    /**
     * Checks on index fragments (user owned and shared) which have not yet been imported into the index and executes ES
     * import/deletion
     */
    private void importIndexFragment(User user, IndexFragmentEntryStatus importTask) {
        try {
            //1. fetch the document
            IndexDocument doc = getDocumentFromStorage(importTask);
            //2. import to ElasticSearch
            this.importToESIndex(doc, user);
            //3. update status in database
            importTask.setStatusType(StatusType.IMPORTED);
            this.entryStatusDao.merge(importTask);
            //4. finally move the serialized Index Document to the encrypted partition
            moveFragmentToEncryptedUserStorage(doc, user, importTask.isUserOwned());
            this.log.debug("Content Import index fragment " + importTask.getDocumentUUID() + " for user " + user.id()
                    + " completed");

        } catch (ContentUpdateException | SearchInstanceException e) {
            this.log.debug("Failed to execute content import task", e);
        }

    }

    private void deleteIndexFragment(User user, IndexFragmentEntryStatus deletionTask) {
        try {
            //1. remove from ElasticSearch
            this.deleteFromESIndex(deletionTask.getDocumentUUID(), user);
            //2. update status in database
            deletionTask.setStatusType(StatusType.DELETED);
            this.entryStatusDao.merge(deletionTask);
            //3. finally delete the serialized Index Document from the encrypted partition
            deleteFragmentFromEncryptedUserStorage(deletionTask.getDocumentUUID(), user, deletionTask.isUserOwned());
            this.log.debug("Content Deletion of index fragment " + deletionTask.getDocumentUUID() + " for user "
                    + user.id() + " completed");

        } catch (ContentUpdateException | SearchInstanceException e) {
            this.log.debug("Failed to execute content deletion task", e);
        }
    }

    /**
     * Takes care of pending content update tasks (e.g. elements waiting for import or deletion in ES) It queries the db
     * to check on pending items, calls ES to execute the operation and finally updates the status within the DB
     * 
     * @param user
     */
    public void executeContentUpdates(User user) {
        List<IndexFragmentEntryStatus> lToImport = this.entryStatusDao.getAllFromUserOfType(user,
                StatusType.WAITING_FOR_IMPORT);
        for (IndexFragmentEntryStatus toImport : lToImport) {
            //execute the import task
            importIndexFragment(user, toImport);
        }

        List<IndexFragmentEntryStatus> lToDelete = this.entryStatusDao.getAllFromUserOfType(user,
                StatusType.WAITING_FOR_DELETION);
        for (IndexFragmentEntryStatus toDelete : lToDelete) {
            //execute the deletion task
            deleteIndexFragment(user, toDelete);
        }
    }

    /**
     * Uses the Elastic Search IndexClient to execute the index operation which imports the IndexDocument to ES
     * 
     * @param doc
     * @param user
     */
    private void importToESIndex(IndexDocument doc, User user) {
        try {
            try (IndexClient indexClient = this.esSetup.createIndexClient(user)) {
                indexClient.index(doc);
                this.log.debug("document indexed by ElasticSearch. userID=" + user.id() + " document: "
                        + Json.serialize(doc));
            }
        } catch (SearchInstanceException e) {
            this.log.error("failed to add IndexDocument " + Json.serialize(doc) + " for userID: " + user.id() + " " + e);
            throw e;
        } catch (IOException e) {
            this.log.error("failed to add IndexDocument " + Json.serialize(doc) + " for userID: " + user.id() + " " + e);
            throw new SearchInstanceException("failed to add IndexDocument for userID: " + user.id(), e);
        }
    }

    /**
     * Uses the Elastic Search IndexClient to execute the delete operation which deletes a specific IndexDocument from
     * ES
     * 
     * @param doc
     * @param user
     */
    private void deleteFromESIndex(UUID docUUID, User user) {
        try {
            try (IndexClient indexClient = this.esSetup.createIndexClient(user)) {
                indexClient.deleteRecordsForUserAndDocumentUUID(docUUID);
                this.log.debug("document " + docUUID + " removed from ElasticSearch for userID=" + user.id());
            }
        } catch (SearchInstanceException e) {
            this.log.error("failed to delete IndexDocument " + docUUID + " for userID: " + user.id() + " " + e);
            throw e;
        }
    }

    private String getMountedTCDriveLetter(User user) {
        RunningIndexUserConfig config = this.runninInstancesDao.findConfigByUser(user);
        if (config != null) {
            return config.getMountedTCDriveLetter();
        }
        throw new ContentUpdateException("No encrypted user space mounted");
    }

    private void moveFragmentToEncryptedUserStorage(IndexDocument doc, User user, boolean userOwned) {
        try {
            if (userOwned) {
                ThemisEncryptedPartition.saveIndexFragment(doc, user,
                        ThemisEncryptedPartition.IndexFragmentType.IMPORTED_USER_OWNED, getMountedTCDriveLetter(user));
                ThemisDataSink.deleteIndexFragment(
                        UUID.fromString(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString()), user,
                        ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);
            } else {
                ThemisEncryptedPartition.saveIndexFragment(doc, user,
                        ThemisEncryptedPartition.IndexFragmentType.IMPORTED_SHARED_WITH_USER,
                        getMountedTCDriveLetter(user));
                ThemisDataSink.deleteIndexFragment(
                        UUID.fromString(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString()), user,
                        ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
            }
        } catch (Exception e) {
            throw new ContentUpdateException("Copying index fragment to encrypted user partition failed", e);
        }
    }

    private void deleteFragmentFromEncryptedUserStorage(UUID docUUID, User user, boolean userOwned) {
        try {
            if (userOwned) {
                ThemisEncryptedPartition.deleteIndexFragment(docUUID, user,
                        ThemisEncryptedPartition.IndexFragmentType.IMPORTED_USER_OWNED, getMountedTCDriveLetter(user));
            } else {
                ThemisEncryptedPartition.deleteIndexFragment(docUUID, user,
                        ThemisEncryptedPartition.IndexFragmentType.IMPORTED_SHARED_WITH_USER,
                        getMountedTCDriveLetter(user));
            }
        } catch (Exception e) {
            throw new ContentUpdateException("deleting index fragment from encrypted user partition failed", e);
        }
    }

    private IndexDocument getDocumentFromStorage(IndexFragmentEntryStatus importTask) {
        IndexDocument doc = null;
        //check where to fetch the IndexDocument from
        if (importTask.isUserOwned()) {
            try {
                doc = ThemisDataSink.getIndexFragment(importTask.getDocumentUUID(), new User(importTask.getUserID()),
                        ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);

            } catch (IOException e) {
                String s = "Error fetching document for import; documentUUID: " + importTask.getDocumentUUID()
                        + " userOwned?: " + importTask.isUserOwned() + " for user: " + importTask.getUserID();
                this.log.error(s, e);
                throw new ContentUpdateException(s, e);
            }
        } else {
            try {
                doc = ThemisDataSink.getIndexFragment(importTask.getDocumentUUID(), new User(importTask.getUserID()),
                        ThemisDataSink.IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);

            } catch (IOException e) {
                String s = "Error fetching document for import; documentUUID: " + importTask.getDocumentUUID()
                        + " userOwned?: " + importTask.isUserOwned() + " for user: " + importTask.getUserID();
                this.log.error(s, e);
                throw new ContentUpdateException(s, e);
            }
        }
        return doc;
    }

    @RequestScoped
    public void startupIndexContentManager() {
        this.log.debug("startup() IndexContentManager (ApplicationScoped) completed");
    }

    @RequestScoped
    public void shutdownIndexContentManager() {
        this.log.debug("shutdown() IndexContentManager (ApplicationScoped) completed");
    }

}
