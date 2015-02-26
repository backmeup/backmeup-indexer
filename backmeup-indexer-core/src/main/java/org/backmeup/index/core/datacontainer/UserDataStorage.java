package org.backmeup.index.core.datacontainer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import javax.enterprise.context.ApplicationScoped;

import org.backmeup.index.UserDataWorkingDir;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.model.User;
import org.backmeup.index.storage.ThemisDataSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logic regarding the data container where we get the crypt file from. This uses a real file provider ("data sink") to
 * delegate the real file handling work to.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@ApplicationScoped
public class UserDataStorage {

    private static final String CRYPT_TEMPLATE = "elasticsearch_userdata_template_TC_150MB.tc";
    private static final String CRYPT_FILE = "elasticsearch_userdata_TC_150MB.tc";

    private final Logger log = LoggerFactory.getLogger(getClass());

    public File getUserStorageCryptContainerFor(User userID) {
        try {
            File f = ThemisDataSink.getIndexTrueCryptContainer(userID);
            this.log.debug("returning existing truecrypt container for user " + userID);
            return f;

        } catch (IOException e) {
            this.log.debug("creating new truecrypt container for user " + userID);
            return createNewUserStorageCryptContainerFor(userID);
        }
    }

    private File createNewUserStorageCryptContainerFor(User userID) {
        // initialize user
        init(userID);

        // try the call again - user now initialized, if error -> fail
        try {

            return ThemisDataSink.getIndexTrueCryptContainer(userID);

        } catch (IOException e1) {
            throw new UserDataStorageException("startupInstance for userID: " + userID + " step1 - failed", e1);
        }
    }

    /**
     * Init a user specific elasticsearch instance i.e. copies the container file and registers it within the
     * themis-datasink
     */
    private void init(User userID) {
        // TODO fix weakness currently all copied TC container files have the
        // same default password as this cannot be changed via TC command line
        // interface. idea: keep default password but encrypt the container file
        try {
            ThemisDataSink.saveIndexTrueCryptContainer(getClass().getClassLoader().getResourceAsStream(CRYPT_TEMPLATE),
                    userID);
        } catch (IOException e) {
            throw new UserDataStorageException("IndexManager init ES instance failed for user" + userID, e);
        }
    }

    public File copyUserStorageCryptContainerToLocalWorkingDir(User userID, File fTCContainerOnDataSink) {
        try {

            return copyTCContainerFileToLocalWorkingDir(fTCContainerOnDataSink, userID);

        } catch (IOException e1) {
            throw new UserDataStorageException("startupInstance for userID: " + userID + " step2 - failed", e1);
        }
    }

    private File copyTCContainerFileToLocalWorkingDir(File fTCContainerOnDataSink, User userID) throws IOException {
        return copyFileUsingChannel(fTCContainerOnDataSink, new File(UserDataWorkingDir.getDir(userID) + "/index/"
                + CRYPT_FILE));
    }

    /**
     * Copies a file from source to dest using FileChannels and creates the file and directory if it does not already
     * exist.
     */
    private File copyFileUsingChannel(File source, File dest) throws IOException {

        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            if (dest.exists()) {
                dest.delete();
            }
            dest.getParentFile().mkdirs();
            dest.createNewFile();

            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            return dest;
        } finally {
            if (sourceChannel != null) {
                sourceChannel.close();
            }
            if (destChannel != null) {
                destChannel.close();
            }
        }
    }

    public void copyCryptContainerDataBackIntoUserStorage(RunningIndexUserConfig runningInstanceConfig) {
        User userID = runningInstanceConfig.getUser();
        try {

            ThemisDataSink.saveIndexTrueCryptContainer(new File(runningInstanceConfig.getMountedContainerLocation()),
                    userID);
            this.log.debug("shutdownInstance for userID: " + userID + " step4 - ok");

        } catch (IOException e) {
            this.log.error("shutdownInstance for userID: " + userID + " step4 - failed", e);
        }
    }

}
