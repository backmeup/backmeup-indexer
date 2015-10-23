package org.backmeup.index;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.model.User;
import org.backmeup.keyserver.client.KeyserverClient;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.model.dto.TokenDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks the DB if an ES instance is up and running for a given user
 *
 */
@ApplicationScoped
public class ActiveUsers {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private RunningIndexUserConfigDao dao;
    @Inject
    private KeyserverClient keyserverClient;

    public boolean isUserActive(User userID) {
        RunningIndexUserConfig config = this.dao.findConfigByUser(userID);
        if (config == null) {
            return false;
        } else {
            return true;
        }
    }

    public List<User> getActiveUsers() {
        List<User> ret = new ArrayList<User>();
        List<RunningIndexUserConfig> configs = this.dao.getAllESInstanceConfigs();
        for (RunningIndexUserConfig config : configs) {
            ret.add(config.getUser());
        }
        return ret;
    }

    public String getKeyserverAuthenticationToken(Long userId) throws IOException {
        //check if the user is currently available on the system - here we find the keyserver token to access the private key
        RunningIndexUserConfig activeUserConfig = this.dao.findConfigByUser(new User(userId));
        if (activeUserConfig == null) {
            String ex = "error retrieving private for userId:" + userId + " - user not currently not active on system";
            this.log.debug(ex);
            throw new IOException(ex);
        }
        //get the keyserver token from the running user configuration
        String userKSToken = activeUserConfig.getKeyServerUserAuthenticationToken();
        if (userKSToken == null) {
            String ex = "error retrieving private for userId:" + userId + " - keyserver token not available";
            this.log.debug(ex);
            throw new IOException(ex);
        }
        return userKSToken;
    }

    public PrivateKey getPrivateKey(Long userId) throws IOException {
        String userKSToken = getKeyserverAuthenticationToken(userId);
        byte[] privatekey;
        try {
            //now get the private key for this user from the keyserver
            AuthResponseDTO auth = this.keyserverClient.authenticateWithInternalToken(TokenDTO.fromTokenString(userKSToken));
            privatekey = this.keyserverClient.getPrivateKey(auth.getToken());
            PrivateKey privateKey = KeyserverClient.decodePrivateKey(privatekey);
            return privateKey;
        } catch (KeyserverException e) {
            String ex = "error retrieving private for userId:" + userId + " from keyserver";
            this.log.debug(ex, e);
            throw new IOException(ex);
        }
    }

    public String getMountedDrive(User user) {
        RunningIndexUserConfig config = this.dao.findConfigByUser(user);
        if (config != null) {
            return config.getMountedTCDriveLetter();
        } else {
            return null;
        }
    }

    protected void setDaoForTesting(RunningIndexUserConfigDao dao) {
        this.dao = dao;
    }

}
