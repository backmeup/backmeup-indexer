package org.backmeup.index.core.truecrypt;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.model.User;
import org.backmeup.keyserver.client.KeyserverClient;
import org.backmeup.keyserver.model.KeyserverException;
import org.backmeup.keyserver.model.dto.AuthResponseDTO;
import org.backmeup.keyserver.model.dto.TokenDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encryption provider handles the encryption related operation but delegates to a low level encryption handler for the
 * real, encryption specific work.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@ApplicationScoped
public class EncryptionProvider {

    @Inject
    private KeyserverClient keyserverClient;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public String mountNextFreeMountPoint(User user, File fTCContainer) {
        // TODO currently when all available drives are in use the system will throw an IOException
        //get the password from keyserver
        String password = null;
        try {
            AuthResponseDTO auth = this.keyserverClient.authenticateWithInternalToken(TokenDTO.fromTokenString(user
                    .getKeyServerInternalToken()));
            password = this.keyserverClient.getIndexKey(auth.getToken());
        } catch (KeyserverException e) {
            throw new EncryptionProviderException("startupInstance for userID: " + user + " step3 - obtaining container password failed", e);
        }
        //now mount the drive
        try {
            String tcMountedDriveLetter = TCMountHandler.mount(fTCContainer, password, TCMountHandler.getSupportedDriveLetters().get(0));
            this.log.debug("Mounted Drive Letter: " + tcMountedDriveLetter + "from: " + fTCContainer.getAbsolutePath());
            return tcMountedDriveLetter;
        } catch (ExceptionInInitializerError | IllegalArgumentException | IOException | InterruptedException e1) {
            throw new EncryptionProviderException("startupInstance for userID: " + user + " step3 - failed", e1);
        }
    }

    public void unmount(RunningIndexUserConfig runningInstanceConfig) {
        User userID = runningInstanceConfig.getUser();
        try {
            String driveLetter = runningInstanceConfig.getMountedTCDriveLetter();
            TCMountHandler.unmount(driveLetter);
            this.log.debug("shutdownInstance for userID: " + userID + " step3 - ok");
        } catch (IllegalArgumentException | ExceptionInInitializerError | IOException | InterruptedException e) {
            this.log.debug("shutdownInstance for userID: " + userID + " step3 - failed", e);
        }
    }

    public void unmountAll() {
        try {
            this.log.debug("cleanupRude: started unmounting all TC instances");
            TCMountHandler.unmountAll();
            this.log.debug("cleanupRude: completed - all TC volumes unmounted");
        } catch (IOException | InterruptedException e) {
            this.log.debug("cleanupRude: unmounting all TC volumes failed", e);
        }
    }

    public File generateNewCryptVolume(User user) throws IOException {
        try {
            AuthResponseDTO auth = this.keyserverClient.authenticateWithInternalToken(TokenDTO.fromTokenString(user
                    .getKeyServerInternalToken()));
            //generate a random password
            String password = generateRandomPassword();
            try {
                //try to generate a TrueCrypt volume (only works in Linux)
                File fVolume = TCMountHandler.generateTrueCryptVolume(150, password);
                //save the password within keyserver
                this.keyserverClient.setIndexKey(auth.getToken(), password);
                this.log.debug(
                        "successfully created new truecrypt volume for user: {} with 150MB capacity and persisted password in keyserver",
                        user.id());
                return fVolume;

            } catch (IOException e2) {
                //in this case remove the password and reset it to default
                password = "12345"; //default PW for TrueCrypt pre-generated template for windows
                //save the password within keyserver
                this.keyserverClient.setIndexKey(auth.getToken(), password);
                throw e2;
            }
        } catch (KeyserverException e) {
            throw new EncryptionProviderException("startupInstance for userID: " + user
                    + " step3 - obtaining or setting container password failed", e);
        }
    }

    private String generateRandomPassword() {
        String ASCII_LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
        String ASCII_UPPERCASE_CHARS = ASCII_LOWERCASE_CHARS.toUpperCase();
        String NUMBERS = "0123456789";
        return RandomStringUtils.random(20, ASCII_LOWERCASE_CHARS + ASCII_UPPERCASE_CHARS + NUMBERS);
    }
}
