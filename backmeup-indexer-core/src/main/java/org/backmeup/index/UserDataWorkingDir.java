package org.backmeup.index;

import java.io.File;

import org.backmeup.index.config.Configuration;
import org.backmeup.index.model.User;
import org.backmeup.index.utils.file.FileUtils;

public class UserDataWorkingDir {

    private static final String USER_ROOT_DIR_CONFIG_KEY = "index.temp.data.home.dir";

    /**
     * Gets the root directory (for index operations) for all users done.
     */
    private static String getRoot() {
        String s = Configuration.getProperty(USER_ROOT_DIR_CONFIG_KEY);
        if (s != null && s.length() > 0 && !s.contains("\"")) {
            File f = new File(s);
            if (f.isDirectory() && f.exists()) {
                return f.getAbsolutePath();
            }
    
            f.mkdirs();
            if (f.isDirectory() && f.exists()) {
                return f.getAbsolutePath();
            }
    
            throw new ExceptionInInitializerError(
                    "index.temp.data.home.dir does not exist or is not accessible to system");
        }
        throw new ExceptionInInitializerError(
                "index.temp.data.home.dir not properly configured within backmeup-indexer.properties");
    }

    /**
     * Gets the user's working directory on the temporary file share to operate the index upon
     */
    public static String getDir(User userID) {
        return getRoot() + "/user" + userID.id();
    }

    /**
     * Deletes the working directory for a given user including all files within it
     */
    public static void deleteLocalWorkingDir(User userID) {
        File f = new File(getDir(userID));
        if (f.exists()) {
            FileUtils.deleteDirectory(f);
        }
    }

}
