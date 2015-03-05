package org.backmeup.index.dal;

import java.util.Date;
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
    IndexFragmentEntryStatus getByEntityId(Long entityId);

    /**
     * Find all status objects on an IndexFragment Entry for a given user. The summary of all status objects reflects
     * the ElasticSearch content a user is able to see
     * 
     * @param userID
     * @return
     */
    public List<IndexFragmentEntryStatus> getAllFromUser(User user);

    /**
     * Find Status on an IndexFragment Entry on a specific type as e.g. waiting_for_import filtered by an underlying
     * user
     * 
     * @param userID
     * @param type
     * @return
     */
    List<IndexFragmentEntryStatus> getAllFromUserOfType(User user, IndexFragmentEntryStatus.StatusType type);

    /**
     * Find Status on an IndexFragment Entry that match one of the given types as e.g. waiting_for_import or imported
     * filtered by an underlying user
     * 
     * @param user
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllFromUserInOneOfTheTypes(User user,
            IndexFragmentEntryStatus.StatusType... types);

    /**
     * Find Status on an IndexFragment Entry on a specific IndexDocument UUID over all users on. Shared documents
     * accross different users have the same documentUUID but with different documents.
     * 
     * @param documentUUID
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByDocumentUUID(UUID documentUUID);

    /**
     * Find Status on an IndexFragment Entry on a specific IndexDocument UUID for a given user.
     * 
     * @param documentUUID
     * @return
     */
    IndexFragmentEntryStatus getByUserAndDocumentUUID(User user, UUID documentUUID);

    /**
     * Is status on an IndexFragment Entry on a specific IndexDocument UUID for a given user existing in DB?
     * 
     * @param documentUUID
     * @return
     */
    boolean isIndexFragmentEntryStatusExisting(User user, UUID documentUUID);

    /**
     * Finds Entry status objects for a given user from a certain backupJobId
     * 
     * @param user
     * @param backupJobId
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserAndBackupJobID(User user, long backupJobId);

    /**
     * Returns Entry status objects for a given user with backupedAtDate before the given querytimestamp
     * 
     * @param user
     * @param timestamp
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserAndBeforeBackupDate(User user, Date backupDate);

    /**
     * Returns Entry status objects for a given user with backupedAtDate after the given querytimestamp
     * 
     * @param user
     * @param timestamp
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserAndAfterBackupDate(User user, Date backupDate);

    /**
     * Find all Status objects over all users that match a certain condition e.g. waiting_for_import.
     * 
     * @param type
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByStatusType(IndexFragmentEntryStatus.StatusType type);

    void deleteAll();

}