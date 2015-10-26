package org.backmeup.index.sharing.execution;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.ActiveUsers;
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
    public void updateStorageFileAccessRights(Long fromUserId, Long withUserId, IndexDocument doc) throws IOException {
        //check if we're using backmeup storage as sink 
        if (isBackmeupSinkStorage(doc)) {
            String filePath = doc.getFields().get("path").toString();
            //check existing file access rights on storage for user and sharing partner
            boolean bAccessFromUser = isFromUserStorageFileAccessRightOK(fromUserId, withUserId, filePath);
            boolean bAccessWithUser = isWithUserStorageFileAccessRightOK(fromUserId, withUserId, filePath);

            if (!bAccessFromUser) {
                this.log.error("error on storage file access rights for owner: " + fromUserId + " on filePath: " + filePath);
            }
            if (!bAccessWithUser) {
                //grant sharing partner access rights on file
                this.log.debug("discovered missing file access rights for sharingpartner: " + withUserId + " on file" + fromUserId + "/"
                        + filePath);
                //call addPermission on storage client
                this.callStorageUpdateFileAccessRights(fromUserId, withUserId, filePath);
            }
        }
    }

    private void callStorageUpdateFileAccessRights(Long fromUserId, Long withUserId, String filePath) throws IOException {
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

    private boolean isFromUserStorageFileAccessRightOK(Long fromUserId, Long withUserid, String filePath) throws IOException {
        try {
            return isStorageFileAccessRightOK(fromUserId, null, filePath);
        } catch (IOException e) {
            this.log.debug("error checking storage access right on " + filePath + " for currUser:" + fromUserId + " with his credentials"
                    + e.toString());
            throw e;
        }
    }

    private boolean isWithUserStorageFileAccessRightOK(Long fromUserId, Long withUserid, String filePath) throws IOException {
        try {
            return isStorageFileAccessRightOK(fromUserId, withUserid, filePath);
        } catch (IOException e) {
            this.log.debug("error checking storage access right on: " + filePath + " for sharingpartner: " + withUserid
                    + "through currUser:" + fromUserId + " credentials" + e.toString());
            throw e;
        }
    }

    private boolean isStorageFileAccessRightOK(Long fromUserId, Long withUserId, String filePath) throws IOException {
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

}
