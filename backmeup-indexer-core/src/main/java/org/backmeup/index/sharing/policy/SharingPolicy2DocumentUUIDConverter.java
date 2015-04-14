package org.backmeup.index.sharing.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.model.User;

/**
 * Translates SharingPolicies into actual IndexFragment UUIDs. e.g. share_all user1 with user2 checks which UUIDs of
 * user1 are currently imported/scheduled for import and therefore need to be shared with user2.
 *
 */
@ApplicationScoped
public class SharingPolicy2DocumentUUIDConverter {

    @Inject
    private IndexFragmentEntryStatusDao entryStatusDao;

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
        }

        return lMissingForSharingPartner;
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
        ret = removeDelta(currUserUUIDs, sharingPUUIDs);
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
        ret = removeDelta(currUserUUIDs, sharingPUUIDs);
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
        ret = removeDelta(currUserUUIDs, sharingPUUIDs);
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
    private List<UUID> removeDelta(List<IndexFragmentEntryStatus> currUserUUIDs,
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
}
