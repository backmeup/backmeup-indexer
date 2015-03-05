package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.sql.Date;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.storage.ThemisDataSink;
import org.backmeup.index.storage.ThemisDataSink.IndexFragmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SharingPolicyExecution {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexFragmentEntryStatusDao entryStatusDao;

    public void executeImportOwner(IndexDocument doc) throws IOException {
        User owner = new User(Long.parseLong(doc.getFields().get(IndexFields.FIELD_OWNER_ID).toString()));
        UUID docUUID = UUID.fromString(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());
        int backupJobID = Integer.valueOf(doc.getFields().get(IndexFields.FIELD_JOB_ID).toString());
        Long timestampBackup = (Long) doc.getFields().get(IndexFields.FIELD_BACKUP_AT);
        Date dateBackupAt = new Date(timestampBackup);

        if (isElementToImport(owner, docUUID)) {
            //distribute to owner
            ThemisDataSink.saveIndexFragment(doc, owner, ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED);
            //create the status entry and persist it in db
            IndexFragmentEntryStatus status = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT, docUUID,
                    owner, owner, backupJobID, dateBackupAt);
            this.entryStatusDao.save(status);
            this.log.debug("distributed and stored owned IndexFragment: " + docUUID.toString() + " for userID: "
                    + owner.id());
        }
    }

    public void executeImportSharingParnter(SharingPolicy policy, IndexDocument doc) throws IOException {
        User shareWithUser = new User(policy.getWithUserID());
        User actualDocOwner = new User(policy.getFromUserID());
        UUID docUUID = UUID.fromString(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());

        //check if we actually need to import this document for this user
        if (isElementToImport(shareWithUser, docUUID)) {

            //1a. check sharing_all including old jobs
            if (policy.getPolicy().equals(SharingPolicies.SHARE_ALL_INKLUDING_OLD)) {
                this.executeImportShareAllInklOld(policy, doc, docUUID, shareWithUser, actualDocOwner);
            }

            //1b. checking share_all but just the ones after a given timestamp
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_ALL_AFTER_NOW)) {
                this.executeImportShareAllAfterNow(policy, doc, docUUID, shareWithUser, actualDocOwner);
            }

            //2. check if we're sharing this backup
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_BACKUP)) {
                this.executeImportShareBackup(policy, doc, docUUID, shareWithUser, actualDocOwner);
            }

            //3. check if we're sharing this specific element/file
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_INDEX_DOCUMENT)) {
                this.executeImportShareDocument(policy, doc, docUUID, shareWithUser, actualDocOwner);
            }
        }
    }

    private boolean isElementToImport(User user, UUID docUUID) {
        IndexFragmentEntryStatus status = this.entryStatusDao.getByUserAndDocumentUUID(user, docUUID);
        if (status == null) {
            //if element does not exist for user -> it is to import
            return true;
        } else if (status.getStatusType() == StatusType.WAITING_FOR_IMPORT) {
            //we've already have a waiting for import statement for this element for this user, don't need to import again
            return false;
        } else if (status.getStatusType() == StatusType.IMPORTED) {
            //we've already an imported element for this user
            return false;
        }
        return true;
    }

    private void executeImportShareAllInklOld(SharingPolicy policy, IndexDocument doc, UUID docUUID,
            User shareWithUser, User actualDocOwner) throws IOException {
        //drop off document in public user drop off zone
        ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
        //create a status object
        createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
        this.log.debug("distributed and stored shared IndexFragment: " + docUUID.toString() + " for userID: "
                + shareWithUser.id() + " policy: " + policy.toString());
    }

    private void executeImportShareAllAfterNow(SharingPolicy policy, IndexDocument doc, UUID docUUID,
            User shareWithUser, User actualDocOwner) throws IOException {
        //check the timestamp if this is newer than the policy timestamp
        if ((policy.getPolicyCreationDate() != null)) {
            Long timestampBackup = (Long) doc.getFields().get(IndexFields.FIELD_BACKUP_AT);
            Date dateBackup = new Date(timestampBackup);
            if (policy.getPolicyCreationDate().before(dateBackup)) {
                //drop off document in public user drop off zone
                ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
                //create a status object
                createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
                this.log.debug("distributed and stored shared IndexFragment: " + docUUID.toString() + " for userID: "
                        + shareWithUser.id() + " policy: " + policy.toString());
            }
        }
    }

    private void executeImportShareBackup(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser,
            User actualDocOwner) throws IOException {
        //check if we're sharing this specific backupjob
        if ((policy.getSharedElementID() != null)
                && (policy.getSharedElementID().equals(doc.getFields().get(IndexFields.FIELD_JOB_ID)))) {
            //drop off document in public user drop off zone
            ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
            //create a status object
            createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
            this.log.debug("distributed and stored shared IndexFragment: " + docUUID.toString() + " for userID: "
                    + shareWithUser.id() + " policy: " + policy.toString());
        }
    }

    private void executeImportShareDocument(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser,
            User actualDocOwner) throws IOException {
        //check if we're sharing this specific element
        if ((policy.getSharedElementID() != null)
                && (policy.getSharedElementID().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID)))) {
            ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
            //create a status object
            createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
            this.log.debug("distributed and stored shared IndexFragment: " + docUUID.toString() + " for userID: "
                    + shareWithUser.id() + " policy: " + policy.toString());
        }
    }

    private void createWaitingForImportEntry(IndexDocument doc, User shareWithUser, User actualDocOwner) {
        UUID docUUID = UUID.fromString(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());
        int backupJobID = Integer.valueOf(doc.getFields().get(IndexFields.FIELD_JOB_ID).toString());
        Long timestampBackup = (Long) doc.getFields().get(IndexFields.FIELD_BACKUP_AT);
        Date dateBackupAt = new Date(timestampBackup);
        //create the status entry and persist it in db
        IndexFragmentEntryStatus status = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT, docUUID,
                shareWithUser, actualDocOwner, backupJobID, dateBackupAt);
        this.entryStatusDao.save(status);
    }

    //TODO AL continue with deletion of elements

}
