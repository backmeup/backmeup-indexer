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

    public void executeImport(SharingPolicy policy, IndexDocument doc) throws IOException {
        User shareWithUser = new User(policy.getWithUserID());
        UUID docUUID = UUID.fromString(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());

        //check if we don't already know this document for this user
        if (!this.entryStatusDao.isIndexFragmentEntryStatusExisting(shareWithUser, docUUID)) {

            //1a. check sharing_all including old jobs
            if (policy.getPolicy().equals(SharingPolicies.SHARE_ALL_INKLUDING_OLD)) {
                this.executeImportShareAllInklOld(policy, doc, docUUID, shareWithUser);
            }

            //1b. checking share_all but just the ones after a given timestamp
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_ALL_AFTER_NOW)) {
                this.executeImportShareAllAfterNow(policy, doc, docUUID, shareWithUser);
            }

            //2. check if we're sharing this backup
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_BACKUP)) {
                this.executeImportShareBackup(policy, doc, docUUID, shareWithUser);
            }

            //3. check if we're sharing this specific element/file
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_INDEX_DOCUMENT)) {
                this.executeImportShareDocument(policy, doc, docUUID, shareWithUser);
            }
        }
    }

    private void executeImportShareAllInklOld(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser)
            throws IOException {
        //drop off document in public user drop off zone
        ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
        //create a status object
        createWaitingForImportEntry(docUUID, shareWithUser);
        this.log.debug("distributed and stored shared IndexFragment: " + docUUID.toString() + " for userID: "
                + shareWithUser.id() + " policy: " + policy.toString());
    }

    private void executeImportShareAllAfterNow(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser)
            throws IOException {
        //check the timestamp if this is newer than the policy timestamp
        if ((policy.getPolicyCreationDate() != null)) {
            Long timestampBackup = (Long) doc.getFields().get(IndexFields.FIELD_BACKUP_AT);
            Date dateBackup = new Date(timestampBackup);
            if (policy.getPolicyCreationDate().before(dateBackup)) {
                //drop off document in public user drop off zone
                ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
                //create a status object
                createWaitingForImportEntry(docUUID, shareWithUser);
                this.log.debug("distributed and stored shared IndexFragment: " + docUUID.toString() + " for userID: "
                        + shareWithUser.id() + " policy: " + policy.toString());
            }
        }
    }

    private void executeImportShareBackup(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser)
            throws IOException {
        //check if we're sharing this specific backupjob
        if ((policy.getSharedElementID() != null)
                && (policy.getSharedElementID().equals(doc.getFields().get(IndexFields.FIELD_JOB_ID)))) {
            //drop off document in public user drop off zone
            ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
            //create a status object
            createWaitingForImportEntry(docUUID, shareWithUser);
            this.log.debug("distributed and stored shared IndexFragment: " + docUUID.toString() + " for userID: "
                    + shareWithUser.id() + " policy: " + policy.toString());
        }
    }

    private void executeImportShareDocument(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser)
            throws IOException {
        //check if we're sharing this specific element
        if ((policy.getSharedElementID() != null)
                && (policy.getSharedElementID().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID)))) {
            ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER);
            //create a status object
            createWaitingForImportEntry(docUUID, shareWithUser);
            this.log.debug("distributed and stored shared IndexFragment: " + docUUID.toString() + " for userID: "
                    + shareWithUser.id() + " policy: " + policy.toString());
        }
    }

    private void createWaitingForImportEntry(UUID docUUID, User shareWithUser) {
        IndexFragmentEntryStatus status = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT, docUUID, false,
                shareWithUser);
        this.entryStatusDao.save(status);
    }

    public void executeDeleteShareAll() {

    }

    public void executeDeleteShareBackupJob() {

    }

    public void executeImportShareBackupJob() {

    }

    //etc

}
