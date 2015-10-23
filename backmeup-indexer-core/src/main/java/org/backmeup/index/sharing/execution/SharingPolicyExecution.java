package org.backmeup.index.sharing.execution;

import java.io.IOException;
import java.security.PublicKey;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.ActiveUsers;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.core.model.IndexFragmentEntryStatus;
import org.backmeup.index.core.model.IndexFragmentEntryStatus.StatusType;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.dal.TaggedCollectionDao;
import org.backmeup.index.dal.UserMappingHelperDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.sharing.policy.SharingPolicies;
import org.backmeup.index.sharing.policy.SharingPolicy;
import org.backmeup.index.storage.ThemisDataSink;
import org.backmeup.index.storage.ThemisDataSink.IndexFragmentType;
import org.backmeup.index.tagging.TaggedCollection;
import org.backmeup.index.utils.file.UserMappingHelper;
import org.backmeup.keyserver.client.KeyserverClient;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.storage.api.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SharingPolicyExecution {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private IndexFragmentEntryStatusDao entryStatusDao;
    @Inject
    private TaggedCollectionDao taggedCollectionDao;
    @Inject
    private KeyserverClient keyserverClient;
    @Inject
    private UserMappingHelperDao userMappingHelperDao;
    @Inject
    private StorageClient storageClient;
    @Inject
    private ActiveUsers activeUsers;

    public void distributeIndexFragmentToSharingParnter(SharingPolicy policy, IndexDocument doc) throws IOException {
        System.out.println(doc.getFields().toString());
        User shareWithUser = new User(policy.getWithUserID());
        //drop off document in public user drop off zone
        ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER, getPublicKey(shareWithUser));

        //START TESTING
        Long fromUserId = policy.getFromUserID();
        boolean isBMUStore = isBackmeupSinkStorage(doc);
        updateFileAccessRights(fromUserId, doc);
        //END TESTING
    }

    /**
     * Check if this document is using the backmeup storage plugin as backup sink
     * 
     * @param doc
     * @return
     */
    //TODO in eigene Klasse raus ziehen
    private boolean isBackmeupSinkStorage(IndexDocument doc) {
        if (doc.getFields().containsKey("backup_sink_plugin_id")) {
            String sink = doc.getFields().get("backup_sink_plugin_id").toString();
            if (sink.equals("org.backmeup.storage")) {
                //we're using the backmeup storage plugin as sink
                return true;
            }
        }
        return false;
    }

    //TODO in eigene Klasse raus ziehen
    private void updateFileAccessRights(Long fromUserId, IndexDocument doc) {
        //check if we're using backmeup storage as sink 
        if (isBackmeupSinkStorage(doc)) {
            //in this case make sure to update the access rights for the sharing partner on the file itself
            //TODO JUST TESTING FOR NOW
            //get the keyserver token from the running user configuration
            String userKSToken;
            UserMappingHelper fromUser;
            try {
                userKSToken = this.activeUsers.getKeyserverAuthenticationToken(fromUserId);
                fromUser = this.userMappingHelperDao.getByBMUUserId(fromUserId);
                String filePath = doc.getFields().get("path").toString();
                try {
                    boolean bAccessRight = this.storageClient.hasFileAccessRights(userKSToken, fromUserId + "", filePath,
                            fromUser.getKsUserId(), fromUser.getBmuUserId());
                    System.out.println("has file access?" + bAccessRight);
                } catch (IOException e) {
                    System.out.println(e.toString());
                }
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    public void executeImportOwner(IndexDocument doc) throws IOException {
        User owner = new User(Long.parseLong(doc.getFields().get(IndexFields.FIELD_OWNER_ID).toString()));
        UUID docUUID = UUID.fromString(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());
        int backupJobID = Integer.valueOf(doc.getFields().get(IndexFields.FIELD_JOB_ID).toString());
        Long timestampBackup = (Long) doc.getFields().get(IndexFields.FIELD_BACKUP_AT);
        Date dateBackupAt = new Date(timestampBackup);

        if (isElementToImport(owner, docUUID)) {
            //distribute to owner
            ThemisDataSink.saveIndexFragment(doc, owner, ThemisDataSink.IndexFragmentType.TO_IMPORT_USER_OWNED, getPublicKey(owner));
            //create the status entry and persist it in db
            IndexFragmentEntryStatus status = new IndexFragmentEntryStatus(StatusType.WAITING_FOR_IMPORT, docUUID, owner, owner,
                    backupJobID, dateBackupAt);
            this.entryStatusDao.save(status);
            this.log.debug("distributed and stored owned IndexFragment: " + docUUID.toString() + " for userID: " + owner.id());
            this.log.debug("waiting_for_import status created. moved owned IndexFragment: " + docUUID.toString()
                    + " to encryped storage. for userID: " + owner.id());
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

            //4. check if we're sharing this document group
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_INDEX_DOCUMENT_GROUP)) {
                this.executeImportShareDocumentGroup(policy, doc, docUUID, shareWithUser, actualDocOwner);
            }

            //5. check if we're sharing this tagged collection
            else if (policy.getPolicy().equals(SharingPolicies.SHARE_TAGGED_COLLECTION)) {
                this.executeImportShareTaggedCollection(policy, doc, docUUID, shareWithUser, actualDocOwner);
            }
        }
    }

    private boolean isElementToImport(User user, UUID docUUID) {
        IndexFragmentEntryStatus status = this.entryStatusDao.getByUserAndDocumentUUID(user, docUUID);
        if (status == null) {
            //if element does not exist for user -> it is to import
            return true;
        } else if (status.getStatusType() == StatusType.WAITING_FOR_IMPORT) {
            //we've already a waiting for import statement for this element for this user, don't need to import again
            return false;
        } else if (status.getStatusType() == StatusType.IMPORTED) {
            //we've already an imported element for this user
            return false;
        }
        return true;
    }

    private boolean isElementToDelete(User user, UUID docUUID) {
        IndexFragmentEntryStatus status = this.entryStatusDao.getByUserAndDocumentUUID(user, docUUID);
        if (status == null) {
            //if element does not exist for user -> not imported, do nothing
            return false;
        } else if (status.getStatusType() == StatusType.WAITING_FOR_DELETION) {
            //we've already a waiting for deletion statement for this element
            return false;
        } else if (status.getStatusType() == StatusType.DELETED) {
            //we've already deleted this element for this user
            return false;
        }
        return true;
    }

    private void executeImportShareAllInklOld(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser, User actualDocOwner)
            throws IOException {
        //drop off document in public user drop off zone
        ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER, getPublicKey(shareWithUser));
        //create a status object
        createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
        this.log.debug("waiting_for_import status created. moved shared IndexFragment: " + docUUID.toString()
                + " to encryped storage. for userID: " + shareWithUser.id() + " policy: " + policy.toString());
    }

    private void executeImportShareAllAfterNow(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser,
            User actualDocOwner) throws IOException {
        //check the timestamp if this is newer than the policy timestamp
        if ((policy.getPolicyCreationDate() != null)) {
            Long timestampBackup = (Long) doc.getFields().get(IndexFields.FIELD_BACKUP_AT);
            Date dateBackup = new Date(timestampBackup);
            if (policy.getPolicyCreationDate().before(dateBackup)) {
                //drop off document in public user drop off zone
                ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER,
                        getPublicKey(shareWithUser));
                //create a status object
                createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
                this.log.debug("waiting_for_import status created. moved shared IndexFragment: " + docUUID.toString()
                        + " to encryped storage. for userID: " + shareWithUser.id() + " policy: " + policy.toString());
            }
        }
    }

    private void executeImportShareBackup(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser, User actualDocOwner)
            throws IOException {
        //check if we're sharing this specific backupjob
        if ((policy.getSharedElementID() != null)
                && (policy.getSharedElementID().equals(doc.getFields().get(IndexFields.FIELD_JOB_ID).toString()))) {
            //drop off document in public user drop off zone
            ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER, getPublicKey(shareWithUser));
            //create a status object
            createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
            this.log.debug("waiting_for_import status created. moved shared IndexFragment: " + docUUID.toString()
                    + " to encryped storage. for userID: " + shareWithUser.id() + " policy: " + policy.toString());
        }
    }

    private void executeImportShareDocument(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser, User actualDocOwner)
            throws IOException {
        //check if we're sharing this specific element
        if ((policy.getSharedElementID() != null)
                && (policy.getSharedElementID().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID)))) {
            ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER, getPublicKey(shareWithUser));
            //create a status object
            createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
            this.log.debug("waiting_for_import status created. moved shared IndexFragment: " + docUUID.toString()
                    + " to encryped storage. for userID: " + shareWithUser.id() + " policy: " + policy.toString());
        }
    }

    private void executeImportShareDocumentGroup(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser,
            User actualDocOwner) throws IOException {
        if (policy.getSharedElementID() != null) {
            //get the sharedElementID which is list of documentUUIDs which were persisted via List.toString();
            String s = policy.getSharedElementID();
            List<String> docsInPolicy = Arrays.asList(s.substring(1, s.length() - 1).split(",\\s*"));

            //iterate over all documentUUIDs in document group from the policy and check if we're sharing this specific element
            for (String docInPol : docsInPolicy) {
                if (docInPol.equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString())) {
                    //drop off document in public user drop off zone
                    ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER,
                            getPublicKey(shareWithUser));
                    //create a status object
                    createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
                    this.log.debug("waiting_for_import status created. moved shared IndexFragment: " + docUUID.toString()
                            + " to encryped storage. for userID: " + shareWithUser.id() + " policy: " + policy.toString());
                }
            }
        }
    }

    private void executeImportShareTaggedCollection(SharingPolicy policy, IndexDocument doc, UUID docUUID, User shareWithUser,
            User actualDocOwner) throws IOException {
        if (policy.getSharedElementID() != null) {
            //get the sharedElementID which is tagged collection ID defining the documents within this collection
            Long collID = Long.valueOf(policy.getSharedElementID());
            TaggedCollection taggedColl = this.taggedCollectionDao.getByEntityId(collID);
            List<UUID> uuidsInPolicy = taggedColl.getDocumentIds();

            //iterate over all documentUUIDs in document group from the policy and check if we're sharing this specific element
            for (UUID docInPol : uuidsInPolicy) {
                if (docInPol.toString().equals(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString())) {
                    //drop off document in public user drop off zone
                    ThemisDataSink.saveIndexFragment(doc, shareWithUser, IndexFragmentType.TO_IMPORT_SHARED_WITH_USER,
                            getPublicKey(shareWithUser));
                    //create a status object
                    createWaitingForImportEntry(doc, shareWithUser, actualDocOwner);
                    this.log.debug("waiting_for_import status created. moved shared IndexFragment: " + docUUID.toString()
                            + " to encryped storage. for userID: " + shareWithUser.id() + " policy: " + policy.toString());
                }
            }
        }
    }

    private void createWaitingForImportEntry(IndexDocument doc, User shareWithUser, User actualDocOwner) {
        createStatusEntry(doc, shareWithUser, actualDocOwner, StatusType.WAITING_FOR_IMPORT);
    }

    private void createStatusEntry(IndexDocument doc, User shareWithUser, User actualDocOwner, StatusType type) {
        UUID docUUID = UUID.fromString(doc.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID).toString());
        int backupJobID = Integer.valueOf(doc.getFields().get(IndexFields.FIELD_JOB_ID).toString());
        Long timestampBackup = (Long) doc.getFields().get(IndexFields.FIELD_BACKUP_AT);
        Date dateBackupAt = new Date(timestampBackup);
        //create the status entry and persist it in db
        IndexFragmentEntryStatus status = new IndexFragmentEntryStatus(type, docUUID, shareWithUser, actualDocOwner, backupJobID,
                dateBackupAt);
        this.entryStatusDao.save(status);
    }

    public void executeDeletionSharingParnter(SharingPolicy policy, UUID docUUID) throws IOException {
        User shareWithUser = new User(policy.getWithUserID());
        //check if we actually need to delete this document for this user
        if (isElementToDelete(shareWithUser, docUUID)) {
            createWaitingForDeletionEntry(docUUID, shareWithUser);
            this.log.debug("marked IndexFragment for deletion: " + docUUID.toString() + " for userID: " + shareWithUser.id() + " policy: "
                    + policy.toString());
        }
    }

    private void createWaitingForDeletionEntry(UUID docUUID, User shareWithUser) {
        IndexFragmentEntryStatus status = this.entryStatusDao.getByUserAndDocumentUUID(shareWithUser, docUUID);
        if (status == null) {
            String s = "missing status entry for user: " + shareWithUser.id() + " and document: " + docUUID;
            this.log.warn(s);
        } else {
            status.setStatusType(StatusType.WAITING_FOR_DELETION);
            //update the status entry in db
            this.entryStatusDao.merge(status);
        }
    }

    private PublicKey getPublicKey(User user) throws IOException {
        byte[] publickey;
        try {
            //TODO AL get the public key here the user's keyserverUserId here. Kept in a mapping table
            UserMappingHelper userHelper = this.userMappingHelperDao.getByBMUUserId(user.id());
            publickey = this.keyserverClient.getPublicKey(userHelper.getKsUserId());
            PublicKey publicKey = KeyserverClient.decodePublicKey(publickey);
            return publicKey;
        } catch (KeyserverException e) {
            String ex = "error retrieving public key for userId:" + user.id() + " from keyserver";
            this.log.debug(ex, e);
            throw new IOException(ex);
        }
    }

}
