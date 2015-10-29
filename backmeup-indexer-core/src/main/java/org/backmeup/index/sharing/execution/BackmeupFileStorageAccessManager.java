package org.backmeup.index.sharing.execution;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.ActiveUsers;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.dal.UserMappingHelperDao;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.utils.file.UserMappingHelper;
import org.backmeup.storage.api.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackmeupFileStorageAccessManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private UserMappingHelperDao userMappingHelperDao;
    @Inject
    private StorageClient storageClient;
    @Inject
    private ActiveUsers activeUsers;

    /**
     * Check if this document is using the backmeup storage plugin as backup sink
     * 
     */
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

    /**
     * Check on existing file access rights both for owner and sharing partner and if file access rights on themis
     * storage are missing grant them due to the existing sharing policy
     * 
     */
    public void addStorageFileAccessRights(Long fromUserId, Long withUserId, IndexDocument doc) throws IOException {
        //check if we're using backmeup storage as sink 
        if (isBackmeupSinkStorage(doc)) {
            String filePath = doc.getFields().get(IndexFields.FIELD_PATH).toString();
            String filePathThumbnail = null;
            //check if there is a thumbnail attached 
            if (doc.getLargeFields().get(IndexFields.FIELD_THUMBNAIL_PATH) != null) {
                filePathThumbnail = doc.getLargeFields().get(IndexFields.FIELD_THUMBNAIL_PATH).toString();
            }

            //check existing file access rights on storage for user and sharing partner
            boolean bAccessFromUser = fromUserHasStorageFileAccessRight(fromUserId, withUserId, filePath);
            boolean bAccessWithUser = withUserHasStorageFileAccessRight(fromUserId, withUserId, filePath);

            if (!bAccessFromUser) {
                this.log.error("error on storage file access rights for owner: " + fromUserId + " on filePath: " + filePath);
            }
            if (!bAccessWithUser) {
                //grant sharing partner access rights on file
                this.log.debug("discovered missing file access rights for sharingpartner: " + withUserId + " on file" + fromUserId + "/"
                        + filePath);
                //call addPermission on storage client
                this.callStorageAddFileAccessRights(fromUserId, withUserId, filePath);

                //finally check on access rights for related thumbnail
                if (filePathThumbnail != null) {
                    boolean bAccessWithUserThumb = withUserHasStorageFileAccessRight(fromUserId, withUserId, filePathThumbnail);
                    if (!bAccessWithUserThumb) {
                        this.log.debug("discovered missing file access rights for sharingpartner: " + withUserId + " on thumbnail"
                                + fromUserId + "/" + filePathThumbnail);
                        //call addPermission on storage client
                        this.callStorageAddFileAccessRights(fromUserId, withUserId, filePathThumbnail);
                    }
                }
            }
        }
    }

    private void callStorageAddFileAccessRights(Long fromUserId, Long withUserId, String filePath) throws IOException {
        try {
            //get the keyserver token from the running user configuration
            String userKSToken;
            UserMappingHelper fromUser, withUser;

            //fromUser must be current user as his authentication token is required
            userKSToken = this.activeUsers.getKeyserverAuthenticationToken(fromUserId);
            //get the keyserverUserIDs for the given bmuUserIDs
            fromUser = this.userMappingHelperDao.getByBMUUserId(fromUserId);
            withUser = this.userMappingHelperDao.getByBMUUserId(withUserId);
            //call the storage client to grant access fromUser withUser on the given file 
            this.storageClient.addFileAccessRights(userKSToken, fromUserId + "", filePath, fromUser.getKsUserId(), fromUser.getBmuUserId(),
                    withUser.getBmuUserId(), withUser.getKsUserId());
        } catch (IOException e) {
            this.log.debug(
                    "error setting storage access rights on file {} for fromUser: {}  with his/her provided credentials for withUser: {} "
                            + e.toString(), filePath, fromUserId, withUserId);
            throw e;
        }

    }

    private boolean fromUserHasStorageFileAccessRight(Long fromUserId, Long withUserid, String filePath) throws IOException {
        try {
            return hasStorageFileAccessRight(fromUserId, null, filePath);
        } catch (IOException e) {
            this.log.debug("error checking storage access right on " + filePath + " for currUser:" + fromUserId + " with his credentials"
                    + e.toString());
            throw e;
        }
    }

    private boolean withUserHasStorageFileAccessRight(Long fromUserId, Long withUserid, String filePath) throws IOException {
        try {
            return hasStorageFileAccessRight(fromUserId, withUserid, filePath);
        } catch (IOException e) {
            this.log.debug("error checking storage access right on: " + filePath + " for sharingpartner: " + withUserid
                    + "through currUser:" + fromUserId + " credentials" + e.toString());
            throw e;
        }
    }

    private boolean hasStorageFileAccessRight(Long fromUserId, Long withUserId, String filePath) throws IOException {
        //get the keyserver token from the running user configuration
        String userKSToken;
        UserMappingHelper fromUser;

        //fromUser must be current user as his authentication token is required
        userKSToken = this.activeUsers.getKeyserverAuthenticationToken(fromUserId);
        //get the keyserverUserID for the given bmuUserId
        fromUser = this.userMappingHelperDao.getByBMUUserId(fromUserId);

        if (withUserId == null) {
            //check access rights for the user himself/herself
            return this.storageClient.hasFileAccessRights(userKSToken, fromUserId + "", filePath, fromUser.getKsUserId(),
                    fromUser.getBmuUserId(), null);
        } else {
            //check access rights for the sharing partner
            return this.storageClient.hasFileAccessRights(userKSToken, fromUserId + "", filePath, fromUser.getKsUserId(),
                    fromUser.getBmuUserId(), withUserId);
        }
    }

    /**
     * Check on existing file access rights both for owner and sharing partner and if file access rights on themis
     * storage are missing grant them due to the existing sharing policy
     * 
     */
    public void removeStorageFileAccessRights(Long fromUserId, Long withUserId, IndexDocument doc) throws IOException {
        //check if we're using backmeup storage as sink 
        if (isBackmeupSinkStorage(doc)) {
            String filePath = doc.getFields().get(IndexFields.FIELD_PATH).toString();
            String filePathThumbnail = null;
            //check if there is a thumbnail attached 
            if (doc.getLargeFields().get(IndexFields.FIELD_THUMBNAIL_PATH) != null) {
                filePathThumbnail = doc.getLargeFields().get(IndexFields.FIELD_THUMBNAIL_PATH).toString();
            }
            //check existing file access rights on storage for user and sharing partner
            boolean bAccessWithUser = withUserHasStorageFileAccessRight(fromUserId, withUserId, filePath);

            if (bAccessWithUser) {
                //revoke sharing partner access rights on this file
                this.log.debug("discovered required file access permission right removal for sharingpartner: " + withUserId + " on file"
                        + fromUserId + "/" + filePath);
                //call removePermission on storage client
                this.callStorageRemoveFileAccessRights(fromUserId, withUserId, filePath);

                //finally check on access rights for related thumbnail
                if (filePathThumbnail != null) {
                    boolean bAccessWithUserThumb = withUserHasStorageFileAccessRight(fromUserId, withUserId, filePathThumbnail);
                    if (!bAccessWithUserThumb) {
                        this.log.debug("discovered required file access permission right removal for sharingpartner: " + withUserId
                                + " on thumbnail" + fromUserId + "/" + filePathThumbnail);
                        //call addPermission on storage client
                        this.callStorageRemoveFileAccessRights(fromUserId, withUserId, filePathThumbnail);
                    }
                }
            }
        }
    }

    private void callStorageRemoveFileAccessRights(Long fromUserId, Long withUserId, String filePath) throws IOException {
        try {
            //get the keyserver token from the running user configuration
            String userKSToken;
            UserMappingHelper withUser;

            //withUser must be current user as his authentication token is required
            userKSToken = this.activeUsers.getKeyserverAuthenticationToken(withUserId);
            //get the keyserverUserIDs for the given bmuUserIDs
            withUser = this.userMappingHelperDao.getByBMUUserId(withUserId);
            //call the storage client to remove access of withUser (sharingpartner) on the given file 
            this.storageClient.removeFileAccessRights(userKSToken, fromUserId + "", filePath, withUser.getKsUserId(),
                    withUser.getBmuUserId(), withUser.getBmuUserId());
        } catch (IOException e) {
            this.log.debug(
                    "error setting storage access rights on file {} for withUser: {}  with his/her provided credentials for withUser: {} "
                            + e.toString(), filePath, fromUserId, withUserId);
            throw e;
        }

    }
}
