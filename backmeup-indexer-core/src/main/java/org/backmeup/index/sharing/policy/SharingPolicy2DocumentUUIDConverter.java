package org.backmeup.index.sharing.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.dal.TaggedCollectionDao;
import org.backmeup.index.model.User;
import org.backmeup.index.tagging.TaggedCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Translates SharingPolicies into actual IndexFragment UUIDs. e.g. share_all user1 with user2 checks which UUIDs of
 * user1 are currently imported/scheduled for import and therefore need to be shared with user2.
 *
 */
@ApplicationScoped
public class SharingPolicy2DocumentUUIDConverter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexFragmentEntryStatusDao entryStatusDao;
    @Inject
    private SharingPolicyManager shPolManager;
    @Inject
    private TaggedCollectionDao taggedCollectionDao;

    public List<UUID> getMissingDeltaToImportForSharingPartner(SharingPolicy policy) {

        User currUser = new User(policy.getFromUserID());
        User sharingP = new User(policy.getWithUserID());

        //translate policy to documents that require import by sharing partner
        List<UUID> lMissingForSharingPartner = new ArrayList<UUID>();
        if (policy.getPolicy() == SharingPolicies.SHARE_ALL_INKLUDING_OLD) {
            lMissingForSharingPartner = calculateImportDeltaPolicyShareAllInkOld(currUser, sharingP);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_ALL_AFTER_NOW) {
            lMissingForSharingPartner = calculateImportDeltaPolicyShareAllAfterPolicyCreationDate(currUser, sharingP,
                    policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_BACKUP) {
            lMissingForSharingPartner = calculateImportDeltaPolicyShareBackup(currUser, sharingP, policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_INDEX_DOCUMENT) {
            lMissingForSharingPartner = calculateImportDeltaPolicyShareIndexDocument(currUser, sharingP, policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_INDEX_DOCUMENT_GROUP) {
            lMissingForSharingPartner = calculateImportDeltaPolicyShareIndexDocumentGroup(currUser, sharingP, policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_TAGGED_COLLECTION) {
            lMissingForSharingPartner = calculateImportDeltaPolicyShareTaggedCollection(currUser, sharingP, policy);
        }

        return lMissingForSharingPartner;
    }

    /**
     * Translates a sharing policy into acutal required documents for the current user as document owner
     * 
     * @param policy
     * @return
     */
    public List<UUID> getDocsInPolicyForOwner(SharingPolicy policy) {
        User currUser = new User(policy.getFromUserID());

        //translate policy to documents
        if (policy.getPolicy() == SharingPolicies.SHARE_ALL_INKLUDING_OLD) {
            return convert(this.entryStatusDao.getAllFromUserInOneOfTheTypesAndByUserAsDocumentOwner(currUser,
                    StatusType.IMPORTED));
        } else if (policy.getPolicy() == SharingPolicies.SHARE_ALL_AFTER_NOW) {
            return convert(this.entryStatusDao.getAllByUserOwnedAndAfterBackupDate(currUser,
                    policy.getPolicyCreationDate(), StatusType.IMPORTED));
        } else if (policy.getPolicy() == SharingPolicies.SHARE_BACKUP) {
            Long backupJobID = Long.valueOf(policy.getSharedElementID());
            return convert(this.entryStatusDao
                    .getAllByUserOwnedAndBackupJob(currUser, backupJobID, StatusType.IMPORTED));
        } else if (policy.getPolicy() == SharingPolicies.SHARE_INDEX_DOCUMENT) {
            List<IndexFragmentEntryStatus> ret = new ArrayList<IndexFragmentEntryStatus>();
            UUID documentUUID = UUID.fromString(policy.getSharedElementID());
            IndexFragmentEntryStatus status = this.entryStatusDao.getByUserOwnedAndDocumentUUID(currUser, documentUUID,
                    StatusType.IMPORTED);
            if (status != null) {
                ret.add(status);
            }
            return convert(ret);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_INDEX_DOCUMENT_GROUP) {
            List<UUID> uuidsInPolicy = getUUIDsFromPolicyShareDocumentGroup(policy);
            //a list of documents the current user has imported, which she/he owns, matching the policy.
            return convert(this.entryStatusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(currUser, currUser,
                    uuidsInPolicy, StatusType.IMPORTED));
        } else if (policy.getPolicy() == SharingPolicies.SHARE_TAGGED_COLLECTION) {
            List<UUID> uuidsInPolicy = getUUIDsFromPolicyShareTaggedCollection(policy);
            //a list of documents the current user has imported, which she/he owns, matching the policy.
            return convert(this.entryStatusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(currUser, currUser,
                    uuidsInPolicy, StatusType.IMPORTED));
        }
        this.log.warn("should never reach this line. Check if policy is missing missing?");
        return null;
    }

    /**
     * Returns the number of documents in a sharing policy for the current user as document owner
     * 
     * @param policy
     * @return
     */
    public int getNumberOfDocsInPolicyForOwner(SharingPolicy policy) {
        List<UUID> entries = getDocsInPolicyForOwner(policy);
        if (entries != null) {
            return entries.size();
        }
        return -1;
    }

    /**
     * Calculate the missing entries which the current user has imported but which are missing at the sharing partners
     * side, according to the Policy ShareAllInkOld
     * 
     * @param currentUser
     * @param sharingPartner
     * @return
     */
    private List<UUID> calculateImportDeltaPolicyShareAllInkOld(User currentUser, User sharingPartner) {
        List<UUID> ret = new ArrayList<UUID>();
        //a list of documents the current user has imported and which she/he owns. Can't share non imported docs.
        List<IndexFragmentEntryStatus> currUserUUIDs = this.entryStatusDao
                .getAllFromUserInOneOfTheTypesAndByUserAsDocumentOwner(currentUser, StatusType.IMPORTED);

        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao
                .getAllFromUserInOneOfTheTypesAndByDocumentOwner(sharingPartner, currentUser, StatusType.IMPORTED,
                        StatusType.WAITING_FOR_IMPORT);

        //calculate delta - remove all elements that have already been imported
        ret = removeImportDelta(currUserUUIDs, sharingPUUIDs);
        return ret;
    }

    /**
     * Calculate the missing entries which the current user has imported but which are missing at the sharing partners
     * side, according to the Policy ShareAllAllAferPolicyCreationDate
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateImportDeltaPolicyShareAllAfterPolicyCreationDate(User currentUser, User sharingPartner,
            SharingPolicy policy) {
        List<UUID> ret = new ArrayList<UUID>();
        //a list of documents the current user has imported, which she/he owns, matching the policy.
        List<IndexFragmentEntryStatus> currUserUUIDs = this.entryStatusDao.getAllByUserOwnedAndAfterBackupDate(
                currentUser, policy.getPolicyCreationDate(), StatusType.IMPORTED);

        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao
                .getAllByUserAndAfterBackupDateAndByDocumentOwner(sharingPartner, currentUser,
                        policy.getPolicyCreationDate(), StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        //calculate delta - remove all elements that have already been imported
        ret = removeImportDelta(currUserUUIDs, sharingPUUIDs);
        return ret;
    }

    /**
     * Calculate the missing entries which the current user has imported but which are missing at the sharing partners
     * side, according to the Policy ShareBackup
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateImportDeltaPolicyShareBackup(User currentUser, User sharingPartner, SharingPolicy policy) {
        List<UUID> ret = new ArrayList<UUID>();
        Long backupJobID = Long.valueOf(policy.getSharedElementID());
        //a list of documents the current user has imported, which she/he owns, matching the policy.
        List<IndexFragmentEntryStatus> currUserUUIDs = this.entryStatusDao.getAllByUserOwnedAndBackupJob(currentUser,
                backupJobID, StatusType.IMPORTED);

        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao.getAllByUserAndBackupJobAndByDocumentOwner(
                sharingPartner, currentUser, backupJobID, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        //calculate delta - remove all elements that have already been imported
        ret = removeImportDelta(currUserUUIDs, sharingPUUIDs);
        return ret;
    }

    /**
     * Calculates the Delta by UUID key comparison and removes elements from currUserUUIDs that already are contained in
     * sharedUserUUIDs and returns the delta as list
     * 
     * @param currUserUUIDs
     * @param sharedUserUUIDs
     * @return
     */
    private List<UUID> removeImportDelta(List<IndexFragmentEntryStatus> currUserUUIDs,
            List<IndexFragmentEntryStatus> sharedUserUUIDs) {
        List<UUID> ret = new ArrayList<UUID>();
        //add all and then remove existing ones
        for (IndexFragmentEntryStatus status : currUserUUIDs) {
            ret.add(status.getDocumentUUID());
        }
        for (IndexFragmentEntryStatus shFragm : sharedUserUUIDs) {
            UUID shUUID = shFragm.getDocumentUUID();
            for (IndexFragmentEntryStatus ownFragm : currUserUUIDs) {
                UUID ownUUID = ownFragm.getDocumentUUID();
                if (ownUUID.equals(shUUID)) {
                    //UUID comparison, if already existing exclude from delta 
                    ret.remove(ownUUID);
                }
            }
        }

        return ret;
    }

    /**
     * Calculates the Delta by UUID key comparison and removes elements from a that already are contained in b and
     * returns the delta as list
     * 
     * @param currUserUUIDs
     * @param sharedUserUUIDs
     * @return
     */
    private List<UUID> removeDeletionDelta(List<IndexFragmentEntryStatus> a, List<UUID> b) {
        List<UUID> ret = new ArrayList<UUID>();
        List<UUID> lAUUUIDs = new ArrayList<UUID>();
        //add all and then remove existing ones
        for (IndexFragmentEntryStatus status : a) {
            ret.add(status.getDocumentUUID());
            lAUUUIDs.add(status.getDocumentUUID());
        }
        for (UUID bUUID : b) {
            if (lAUUUIDs.contains(bUUID)) {
                //UUID comparison, if already existing exclude from delta 
                ret.remove(bUUID);
            }
        }
        return ret;
    }

    /**
     * Calculate the missing entries which the current user has imported but which are missing at the sharing partners
     * side, according to the Policy ShareIndexDocument
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateImportDeltaPolicyShareIndexDocument(User currentUser, User sharingPartner,
            SharingPolicy policy) {
        List<UUID> ret = new ArrayList<UUID>();
        UUID documentUUID = UUID.fromString(policy.getSharedElementID());
        //a list of documents the current user has imported, which she/he owns, matching the policy.
        IndexFragmentEntryStatus currUserStatus = this.entryStatusDao.getByUserOwnedAndDocumentUUID(currentUser,
                documentUUID, StatusType.IMPORTED);

        if (currUserStatus == null) {
            return ret;
        } else {
            //a list of documents the sharing partner for this user has currently imported or will import
            IndexFragmentEntryStatus sharingPStatus = this.entryStatusDao.getByUserAndDocumentUUIDByDocumentOwner(
                    sharingPartner, currentUser, documentUUID, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);
            if (sharingPStatus == null) {
                //calculate delta - remove all elements that have already been imported
                ret.add(currUserStatus.getDocumentUUID());
            }
        }
        return ret;
    }

    /**
     * Calculate the missing entries which the current user has imported but which are missing at the sharing partners
     * side, according to the Policy ShareIndexDocumentGroup
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateImportDeltaPolicyShareIndexDocumentGroup(User currentUser, User sharingPartner,
            SharingPolicy policy) {
        List<UUID> ret = new ArrayList<UUID>();
        //get the sharedElementID which is list of documentUUIDs which were persisted via List.toString();
        List<UUID> uuidsInPolicy = getUUIDsFromPolicyShareDocumentGroup(policy);

        //a list of documents the current user has imported, which she/he owns, matching the policy.
        List<IndexFragmentEntryStatus> currUserUUIDs = this.entryStatusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(
                currentUser, currentUser, uuidsInPolicy, StatusType.IMPORTED);

        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(
                sharingPartner, currentUser, uuidsInPolicy, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        //calculate delta - remove all elements that have already been imported
        ret = removeImportDelta(currUserUUIDs, sharingPUUIDs);
        return ret;
    }

    /**
     * For the policy share DocumentGroup the sharedElementID is a list of documentUUIDs which were persisted via
     * List.toString(); Extract and return as List<UUID>
     * 
     * @param policy
     * @return
     */
    private List<UUID> getUUIDsFromPolicyShareDocumentGroup(SharingPolicy policy) {
        //
        String s = policy.getSharedElementID();
        List<String> docsInPolicy = Arrays.asList(s.substring(1, s.length() - 1).split(",\\s*"));
        List<UUID> uuidsInPolicy = new ArrayList<UUID>();
        //need to convert from String to UUID
        for (String docInPolicy : docsInPolicy) {
            uuidsInPolicy.add(UUID.fromString(docInPolicy));
        }
        return uuidsInPolicy;
    }

    /**
     * Calculate the missing entries which the current user has imported but which are missing at the sharing partners
     * side, according to the Policy ShareTaggedCollection
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateImportDeltaPolicyShareTaggedCollection(User currentUser, User sharingPartner,
            SharingPolicy policy) {
        List<UUID> ret = new ArrayList<UUID>();
        //get the documentUUIDs within this policy which are defined by the tagged collection
        List<UUID> uuidsInPolicy = getUUIDsFromPolicyShareTaggedCollection(policy);

        //a list of documents the current user has imported, which she/he owns, matching the policy.
        List<IndexFragmentEntryStatus> currUserUUIDs = this.entryStatusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(
                currentUser, currentUser, uuidsInPolicy, StatusType.IMPORTED);

        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(
                sharingPartner, currentUser, uuidsInPolicy, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        //calculate delta - remove all elements that have already been imported
        ret = removeImportDelta(currUserUUIDs, sharingPUUIDs);
        return ret;
    }

    /**
     * For the policy share TaggedCollection the sharedElementID is the taggedCollection's ID which is used to query the
     * DB on elements contained within this collection
     * 
     * @param policy
     * @return
     */
    private List<UUID> getUUIDsFromPolicyShareTaggedCollection(SharingPolicy policy) {
        //
        Long collID = Long.valueOf(policy.getSharedElementID());
        TaggedCollection taggedColl = this.taggedCollectionDao.getByEntityId(collID);
        List<UUID> uuidsInPolicy = taggedColl.getDocumentIds();
        return uuidsInPolicy;
    }

    public List<UUID> getMissingDeltaToDeleteForSharingPartner(SharingPolicy policy) {

        User currUser = new User(policy.getFromUserID());
        User sharingP = new User(policy.getWithUserID());

        //translate policy to documents that require deletion by sharing partner
        List<UUID> lMissingForSharingPartner = new ArrayList<UUID>();
        if (policy.getPolicy() == SharingPolicies.SHARE_ALL_INKLUDING_OLD) {
            lMissingForSharingPartner = calculateDeletionDeltaPolicyShareAllInkOld(currUser, sharingP, policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_ALL_AFTER_NOW) {
            lMissingForSharingPartner = calculateDeletionDeltaPolicyShareAllAfterPolicyCreationDate(currUser, sharingP,
                    policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_BACKUP) {
            lMissingForSharingPartner = calculateDeletionDeltaPolicyShareBackup(currUser, sharingP, policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_INDEX_DOCUMENT) {
            lMissingForSharingPartner = calculateDeletionDeltaPolicyShareIndexDocument(currUser, sharingP, policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_INDEX_DOCUMENT_GROUP) {
            lMissingForSharingPartner = calculateDeletionDeltaPolicyShareIndexDocumentGroup(currUser, sharingP, policy);
        } else if (policy.getPolicy() == SharingPolicies.SHARE_TAGGED_COLLECTION) {
            lMissingForSharingPartner = calculateDeletionDeltaPolicyShareTaggedCollection(currUser, sharingP, policy);
        }

        return lMissingForSharingPartner;
    }

    /**
     * Calculate the missing entries which have been imported but revoked by removing a the sharing policy and therefore
     * require deletion at the sharing partners side, according to the Policy ShareAllInkOld
     * 
     * @param currentUser
     * @param sharingPartner
     * @return
     */
    private List<UUID> calculateDeletionDeltaPolicyShareAllInkOld(User currentUser, User sharingPartner, SharingPolicy p) {
        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao
                .getAllFromUserInOneOfTheTypesAndByDocumentOwner(sharingPartner, currentUser, StatusType.IMPORTED,
                        StatusType.WAITING_FOR_IMPORT);

        //some of those documents may still be shared by a different policy.
        List<UUID> stillRequired = getDocumentsRequiredForUserExludingAGivenPolicy(currentUser, sharingPartner, p);

        //return the uuids
        return removeDeletionDelta(sharingPUUIDs, stillRequired);
    }

    /**
     * Calculate the entries which require deletion for the sharing partner according to removal of the Policy
     * ShareAllAllAferPolicyCreationDate
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateDeletionDeltaPolicyShareAllAfterPolicyCreationDate(User currentUser,
            User sharingPartner, SharingPolicy policy) {
        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao
                .getAllByUserAndAfterBackupDateAndByDocumentOwner(sharingPartner, currentUser,
                        policy.getPolicyCreationDate(), StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        //get a list of documents still required by all other policies
        List<UUID> stillRequired = getDocumentsRequiredForUserExludingAGivenPolicy(currentUser, sharingPartner, policy);

        //return the uuids
        return removeDeletionDelta(sharingPUUIDs, stillRequired);
    }

    /**
     * Calculate the entries which require deletion for the sharing partner according to removal of the Sharing Policy
     * ShareBackup
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateDeletionDeltaPolicyShareBackup(User currentUser, User sharingPartner,
            SharingPolicy policy) {
        Long backupJobID = Long.valueOf(policy.getSharedElementID());
        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao.getAllByUserAndBackupJobAndByDocumentOwner(
                sharingPartner, currentUser, backupJobID, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        //get a list of documents still required by all other policies
        List<UUID> stillRequired = getDocumentsRequiredForUserExludingAGivenPolicy(currentUser, sharingPartner, policy);

        //return the uuids
        return removeDeletionDelta(sharingPUUIDs, stillRequired);
    }

    /**
     * Calculate the entries which require deletion for the sharing partner according to the removal of the Sharing
     * Policy ShareIndexDocument
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateDeletionDeltaPolicyShareIndexDocument(User currentUser, User sharingPartner,
            SharingPolicy policy) {
        List<UUID> ret = new ArrayList<UUID>();
        UUID documentUUID = UUID.fromString(policy.getSharedElementID());
        //a list of documents the current user has imported, which she/he owns, matching the policy.
        IndexFragmentEntryStatus sharingPStatus = this.entryStatusDao.getByUserAndDocumentUUIDByDocumentOwner(
                sharingPartner, currentUser, documentUUID, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        if (sharingPStatus == null) {
            return ret;
        } else {
            //a list of documents the sharing partner for this user has currently imported or will import
            sharingPStatus = this.entryStatusDao.getByUserAndDocumentUUIDByDocumentOwner(sharingPartner, currentUser,
                    documentUUID, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

            //get a list of documents still required by all other policies
            List<UUID> stillRequired = getDocumentsRequiredForUserExludingAGivenPolicy(currentUser, sharingPartner,
                    policy);

            if (!stillRequired.contains(sharingPStatus.getDocumentUUID())) {
                ret.add(sharingPStatus.getDocumentUUID());
            }
        }

        return ret;
    }

    /**
     * Calculate the entries which require deletion for the sharing partner according to the removal of the Sharing
     * Policy ShareIndexDocumentGroup
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateDeletionDeltaPolicyShareIndexDocumentGroup(User currentUser, User sharingPartner,
            SharingPolicy policy) {
        //get the sharedElementID which is list of documentUUIDs which were persisted via List.toString();
        List<UUID> uuidsInPolicy = getUUIDsFromPolicyShareDocumentGroup(policy);

        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(
                sharingPartner, currentUser, uuidsInPolicy, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        //get a list of documents still required by all other policies
        List<UUID> stillRequired = getDocumentsRequiredForUserExludingAGivenPolicy(currentUser, sharingPartner, policy);

        //return the uuids
        return removeDeletionDelta(sharingPUUIDs, stillRequired);
    }

    /**
     * Calculate the entries which require deletion for the sharing partner according to the removal of the Sharing
     * Policy ShareTaggedCollection
     * 
     * @param currentUser
     * @param sharingPartner
     * @param policy
     * @return
     */
    private List<UUID> calculateDeletionDeltaPolicyShareTaggedCollection(User currentUser, User sharingPartner,
            SharingPolicy policy) {
        //get documentUUIDs exposed by this policy and defined within the tagged collection
        List<UUID> uuidsInPolicy = getUUIDsFromPolicyShareTaggedCollection(policy);

        //a list of documents the sharing partner for this user has currently imported or will import
        List<IndexFragmentEntryStatus> sharingPUUIDs = this.entryStatusDao.getAllByUserAndDocumentUUIDsByDocumentOwner(
                sharingPartner, currentUser, uuidsInPolicy, StatusType.IMPORTED, StatusType.WAITING_FOR_IMPORT);

        //get a list of documents still required by all other policies
        List<UUID> stillRequired = getDocumentsRequiredForUserExludingAGivenPolicy(currentUser, sharingPartner, policy);

        //return the uuids
        return removeDeletionDelta(sharingPUUIDs, stillRequired);
    }

    /**
     * Extracts and returns the UUIDs from the IndexFragmentEntryStatus list
     * 
     * @param lEntryStatus
     * @return
     */
    private List<UUID> convert(List<IndexFragmentEntryStatus> lEntryStatus) {
        List<UUID> ret = new ArrayList<UUID>();
        for (IndexFragmentEntryStatus entry : lEntryStatus) {
            UUID shUUID = entry.getDocumentUUID();
            ret.add(shUUID);
        }
        return ret;
    }

    /**
     * Returns a list of all document uuids required by all available policies exluding the provided one
     * 
     * @param policies
     * @return
     */
    public List<UUID> getDocumentsRequiredForUserExludingAGivenPolicy(User fromUser, User withUser, SharingPolicy policy) {
        List<UUID> ret = new ArrayList<UUID>();
        List<SharingPolicy> lPolicies = this.shPolManager.getAllActivePoliciesBetweenUsers(fromUser, withUser);
        for (SharingPolicy p : lPolicies) {
            if (p.getId() == policy.getId()) {
                //we're excluding/skipping this policy
            } else {
                //iterate over all policies and add the uuids
                List<UUID> docUUIDs = getDocsInPolicyForOwner(p);
                for (UUID docUUID : docUUIDs) {
                    if (!ret.contains(docUUID)) {
                        //add to the return list if now already contained
                        ret.add(docUUID);
                    }
                }
            }
        }
        return ret;
    }
}
