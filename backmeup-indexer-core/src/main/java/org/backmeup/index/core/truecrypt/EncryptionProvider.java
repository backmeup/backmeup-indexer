package org.backmeup.index.core.truecrypt;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encryption provider handles the encryption related operation but delegates to
 * a low level encryption handler for the real, encryption specific work.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@ApplicationScoped
public class EncryptionProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public String getNextFreeMountPoint(User userID, File fTCContainer) {
        // TODO currently when all available drives are in use the system will throw an IOException
        String password = "12345";

        try {
            String tcMountedDriveLetter = TCMountHandler.mount(fTCContainer, password, TCMountHandler.getSupportedDriveLetters().get(0));
            this.log.debug("Mounted Drive Letter: " + tcMountedDriveLetter + "from: " + fTCContainer.getAbsolutePath());
            return tcMountedDriveLetter;
        } catch (ExceptionInInitializerError | IllegalArgumentException | IOException | InterruptedException e1) {
            throw new EncryptionProviderException("startupInstance for userID: " + userID + " step3 - failed", e1);
        }
    }

    public void unmount(User userID, RunningIndexUserConfig runningInstanceConfig) {
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

}
