package org.backmeup.index.dal;

import java.util.List;
import java.util.UUID;

import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.model.User;

/**
 * The IndexFragmentEntryStatusDao contains all database relevant operations for the model class
 * IndexFragmentEntryStatus It is used to fetch the persistent information regarding the status on index data import /
 * deletion operations
 */
public interface IndexFragmentEntryStatusDao extends BaseDao<IndexFragmentEntryStatus> {

    /**
     * Find an element by its DB entity id.
     * 
     * @param entityId
     * @return
     */
    IndexFragmentEntryStatus findIndexFragmentEntryStatustByEntityId(Long entityId);

    /**
     * Find all status objects on an IndexFragment Entry for a given user The summary of all status objects reflects the
     * ElasticSearch content a user is able to see
     * 
     * @param userID
     * @return
     */
    List<IndexFragmentEntryStatus> getAllIndexFragmentEntryStatus(User userID);

    /**
     * Find Status on an IndexFragment Entry on a specific type as e.g. waiting_for_import filtered by an underlying
     * user
     * 
     * @param userID
     * @param type
     * @return
     */
    List<IndexFragmentEntryStatus> getAllIndexFragmentEntryStatus(User userID, IndexFragmentEntryStatus.StatusType type);

    /**
     * Find Status on an IndexFragment Entry on a specific IndexDocument UUID over all users on. Shared documents
     * accross different users have the same documentUUID but with different documents.
     * 
     * @param documentUUID
     * @return
     */
    List<IndexFragmentEntryStatus> getAllIndexFragmentEntryStatus(UUID documentUUID);

    /**
     * Find all Status objects over all users that match a certain condition e.g. waiting_for_import.
     * 
     * @param type
     * @return
     */
    List<IndexFragmentEntryStatus> getAllIndexFragmentEntryStatus(IndexFragmentEntryStatus.StatusType type);

    void deleteAll();

}