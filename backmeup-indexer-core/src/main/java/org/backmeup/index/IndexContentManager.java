package org.backmeup.index;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.index.api.IndexClient;
import org.backmeup.index.core.elasticsearch.SearchInstanceException;
import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ElasticSearchIndexClient;
import org.backmeup.index.serializer.Json;
import org.backmeup.index.sharing.execution.ContentUpdateException;
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
    private IndexFragmentEntryStatusDao dao;

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
            //fetch the document
            IndexDocument doc = getDocumentFromStorage(importTask);
            //import to ElasticSearch
            this.importToESIndex(doc, user);
            //update status in database
            importTask.setStatusType(StatusType.IMPORTED);
            this.dao.merge(importTask);

        } catch (ContentUpdateException | SearchInstanceException e) {
            this.log.debug("Failed to execute content import task", e);
        }

    }

    private void deleteIndexFragment(User user, IndexFragmentEntryStatus importTask) {
        //TODO 
    }

    /**
     * Takes care of pending content update tasks (e.g. elements waiting for import or deletion in ES) It queries the db
     * to check on pending items, calls ES to execute the operation and finally updates the status within the DB
     * 
     * @param user
     */
    public void executeContentUpdates(User user) {
        List<IndexFragmentEntryStatus> lToImport = this.dao.getAllIndexFragmentEntryStatus(user,
                StatusType.WAITING_FOR_IMPORT);
        for (IndexFragmentEntryStatus toImport : lToImport) {
            //execute the import task
            importIndexFragment(user, toImport);
        }

        List<IndexFragmentEntryStatus> lToDelete = this.dao.getAllIndexFragmentEntryStatus(user,
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
            try (IndexClient indexClient = new ElasticSearchIndexClient(user,
                    this.indexManager.initAndCreateAndDoEverthing(user))) {
                indexClient.index(doc);
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
     * Uses the Elastic Search IndexClient to execute the index operation which imports the IndexDocument to ES
     * 
     * @param doc
     * @param user
     */
    private void deleteFromESIndex(IndexDocument doc, User user) {
        //TODO still need to implement the indexClient.delete(doc) operation
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

}
