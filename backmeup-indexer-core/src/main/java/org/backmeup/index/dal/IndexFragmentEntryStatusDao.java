package org.backmeup.index.dal;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
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
     * Find Status on an IndexFragment Entries that match one of the given types as e.g. waiting_for_import or imported
     * filtered by an underlying user
     * 
     * @param user
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllFromUserInOneOfTheTypes(User user,
            IndexFragmentEntryStatus.StatusType... types);

    /**
     * Find Status on an IndexFragment Entries that match one of the given types as e.g. waiting_for_import or imported
     * filtered by an underlying user and filtered by the actual document owner
     * 
     * @param user
     * @param actualDocumentOwner
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllFromUserInOneOfTheTypesAndByDocumentOwner(User user, User actualDocumentOwner,
            IndexFragmentEntryStatus.StatusType... types);

    /**
     * Find Status on an IndexFragment Entries that match one of the given types as e.g. waiting_for_import or imported
     * filtered by an underlying user and by document owner where the given user is the actual document owner
     * 
     * @param user
     * @param actualDocumentOwner
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllFromUserInOneOfTheTypesAndByUserAsDocumentOwner(User user,
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
     * Find Status on IndexFragment Entries that match the specified IndexDocument UUIDs for a given user as owner and
     * that match the given policy types.
     * 
     * @param user
     * @param documentUUIDs
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getByUserOwnedAndDocumentUUIDs(User user, List<UUID> documentUUIDs,
            StatusType... types);

    /**
     * Find Status on IndexFragment Entries that match the specified IndexDocument UUIDs for a given user (both owned
     * and imported sharings) that match the given policy types and Status.
     * 
     * @param user
     * @param documentUUIDs
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getByUserOwnedAndImportedSharingsAndByDocumentUUIDs(User user,
            List<UUID> documentUUIDs, StatusType... types);

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
     * Get all Status Entries of a given user where he/she is the owner, the backup date is before a given date and the
     * StatusType matches one of the given types
     * 
     * @param user
     * @param date
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserOwnedAndBeforeBackupDate(User user, Date date, StatusType... types);

    /**
     * Get all Status Entries of a given user matching a given actual document owner, the backup date is before a given
     * date and the StatusType matches one of the given types
     * 
     * @param user
     * @param date
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserAndBeforeBackupDateAndByDocumentOwner(User user, User actualDocOwner,
            Date date, StatusType... types);

    /**
     * Returns Entry status objects for a given user with backupedAtDate after the given query date
     * 
     * @param user
     * @param timestamp
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserAndAfterBackupDate(User user, Date backupDate);

    /**
     * Get all Status Entries of a given user where he/she is the owner, the backup date is after a given date and the
     * StatusType matches one of the given types
     * 
     * @param user
     * @param date
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserOwnedAndAfterBackupDate(User user, Date date, StatusType... types);

    /**
     * Get all Status Entries of a given user matching a given actual document owner, the backup date is before a given
     * date and the StatusType matches one of the given types
     * 
     * @param user
     * @param actualDocOwner
     * @param date
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserAndAfterBackupDateAndByDocumentOwner(User user, User actualDocOwner,
            Date date, StatusType... types);

    /**
     * Get all Status Entries of a given user where he/she is the owner, the backup date is before a given date and the
     * StatusType matches one of the given types
     * 
     * @param user
     * @param date
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserOwnedAndBackupJob(User user, Long backupJobID, StatusType... types);

    /**
     * Get all Status Entries of a given user matching a given actual document owner, the backup date is before a given
     * date and the StatusType matches one of the given types
     * 
     * @param user
     * @param actualDocOwner
     * @param backupJobID
     * @param types
     * @return
     */
    List<IndexFragmentEntryStatus> getAllByUserAndBackupJobAndByDocumentOwner(User user, User actualDocOwner,
            Long backupJobID, StatusType... types);

    /**
     * Get the Status Entry for a user who is the actual document owner, matching the document uuid and the StatusType
     * matches one of the given types
     * 
     * @param user
     * @param documentUUID
     * @param types
     * @return
     */
    IndexFragmentEntryStatus getByUserOwnedAndDocumentUUID(User user, UUID documentUUID, StatusType... types);

    /**
     * Get the Status Entry for a user, matching the document uuid, the actual owner and the StatusType matches one of
     * the given types
     * 
     * @param user
     * @param actualDocOwner
     * @param documentUUID
     * @param types
     * @return
     */
    IndexFragmentEntryStatus getByUserAndDocumentUUIDByDocumentOwner(User user, User actualDocOwner, UUID documentUUID,
            StatusType... types);

    /**
     * Get the Status Entries for a user, matching the document uuids, the actual owner and the StatusType matches one
     * of the given types
     * 
     * @param user
     * @param actualDocOwner
     * @param documentUUIDs
     * @param types
     * @return
     */
    public List<IndexFragmentEntryStatus> getAllByUserAndDocumentUUIDsByDocumentOwner(User user, User actualDocOwner,
            List<UUID> documentUUIDs, StatusType... types);

    /**
     * Find all Status objects over all users that match a certain condition e.g. waiting_for_import.
     * 
     * @param type
     * @return
     */
    public List<IndexFragmentEntryStatus> getAllByStatusType(IndexFragmentEntryStatus.StatusType type);

    void deleteAll();

}