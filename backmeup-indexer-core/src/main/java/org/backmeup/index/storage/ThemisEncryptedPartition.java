package org.backmeup.index.storage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;
import org.backmeup.index.utils.cmd.CommandLineUtils;

/**
 * Simplifies access to the already mounted TrueCrypt partition for a given user
 */
public class ThemisEncryptedPartition {

    public enum IndexFragmentType {
        IMPORTED_USER_OWNED("imported/userowned/"), IMPORTED_SHARED_WITH_USER("imported/sharedwithuser/");

        private String storage_location;

        IndexFragmentType(String location) {
            this.storage_location = location;
        }

        private String getStorageLocation() {
            return this.storage_location;
        }
    }

    /**
     * Persists an IndexDocument within the user's mounted storage partition. Distinguishes between owned and shared
     * IndexDocuments
     */
    public static UUID saveIndexFragment(IndexDocument indexFragment, User user, IndexFragmentType type, String mountedDrive)
            throws IOException {

        if (indexFragment == null) {
            throw new IOException("IndexDocument may not be null");
        }
        if (type == null) {
            throw new IOException("The IndexFragmentType location type may not be null");
        }

        // the object's UUID. same objects across multiple users have same UUID
        UUID uuid = null;
        if (indexFragment.getFields().containsKey(IndexFields.FIELD_INDEX_DOCUMENT_UUID)) {
            // existing object must have already been assigned an UUID
            String uuidString = (String) indexFragment.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID);
            uuid = UUID.fromString(uuidString);
        } else {
            throw new IOException("The IndexFragment must have a document UUID assigned");
        }

        // serialize the IndexDocument to JSON
        String serializedIndexDoc = Json.serialize(indexFragment);
        if (serializedIndexDoc != null) {

            File f = new File(getIndexFragmentStorageZone(mountedDrive) + "/" + type.getStorageLocation() + uuid + ".serindexdocument");
            if (!f.getParentFile().exists()) {
                mkDirs(f.getParentFile());
            }
            FileUtils.writeStringToFile(f, serializedIndexDoc);
            return uuid;

        }
        throw new IOException("Error persisting serialized IndexDocument in encrypted partition"
                + getIndexFragmentStorageZone(mountedDrive) + "/" + type.getStorageLocation() + uuid + ".serindexdocument"
                + " for userID: " + user.id());
    }

    /**
     * Retrieves an IndexDocument within the user's mounted storage partition. Distinguishes between owned and shared
     * IndexDocuments
     * 
     * @param objectID
     * @param user
     * @param type
     * @return
     * @throws IOException
     */
    public static IndexDocument getIndexFragment(UUID objectID, User user, IndexFragmentType type, String mountedDrive) throws IOException {
        File f = getIndexFragmentFile(objectID, user, type, mountedDrive);

        if (user.id() > -1 && (f.exists() && f.canRead())) {
            List<String> lines = FileUtils.readLines(f, "UTF-8");

            String serObject = "";
            for (String l : lines) {
                serObject += l;
            }
            // deserialize the object
            IndexDocument indexDoc = Json.deserialize(serObject, IndexDocument.class);

            return indexDoc;
        }

        throw new IOException("Error getting index fragment: " + getIndexFragmentStorageZone(mountedDrive) + "/"
                + type.getStorageLocation() + objectID + ".serindexdocument" + ", file exists? " + f.exists() + ", file is readable? "
                + f.canRead());
    }

    private static File getIndexFragmentFile(UUID objectID, User user, IndexFragmentType type, String mountedDrive) throws IOException {
        return new File(getIndexFragmentStorageZone(mountedDrive) + "/" + type.getStorageLocation() + objectID + ".serindexdocument");
    }

    /**
     * Retrieves an IndexDocument within the user's mounted storage partition. Distinguishes between owned and shared
     * IndexDocuments
     * 
     * @param objectID
     * @param user
     * @param type
     * @return
     * @throws IOException
     */
    public static void deleteIndexFragment(UUID objectID, User user, IndexFragmentType type, String mountedDrive) throws IOException {
        File f = getIndexFragmentFile(objectID, user, type, mountedDrive);

        if (user.id() > -1 && (f.exists() && f.canRead())) {
            f.delete();
        } else {
            throw new IOException("Error deleting index fragment: " + getIndexFragmentStorageZone(mountedDrive) + "/"
                    + type.getStorageLocation() + objectID + ".serindexdocument" + ", file exists? " + f.exists() + ", file is readable? "
                    + f.canRead());
        }
    }

    private static String getIndexFragmentStorageZone(String driveLetter) throws IOException {
        return getMountedDriveRoot(driveLetter) + "/storagezone";
    }

    /**
     * Returns the root directory for the user's mounted encrypted storage space
     */
    private static String getMountedDriveRoot(String driveLetter) throws IOException {
        File f = null;

        if (SystemUtils.IS_OS_WINDOWS) {
            // note: File.listRoots() to list drives is windows specific
            if (driveLetter.contains(":")) {
                f = new File(driveLetter);
            } else {
                f = new File(driveLetter + ":");
            }
        }

        if (SystemUtils.IS_OS_LINUX) {
            f = new File(driveLetter);
        }
        if (f.isDirectory() && f.exists()) {
            return f.getAbsolutePath();
        } else {
            throw new IOException("Mounted data partition: " + driveLetter + " not accessible");
        }
    }

    private static void mkDirs(File f) {
        if (SystemUtils.IS_OS_LINUX) {
            String command = "sudo mkdir -p " + f.getAbsolutePath();
            try {
                int exitVal = CommandLineUtils.executeCommandLine(command, 2, TimeUnit.SECONDS);
                if (exitVal != 0) {
                    throw new IllegalArgumentException("error executing command " + command + " exit value: " + exitVal);
                }
                //encrypted partition runs as root user. change owner of the top level folder and all files in it 
                //to tomcat7 user and group so the application can read/create files
                CommandLineUtils.chownRTomcat7(f);
            } catch (IOException e) {
                throw new IllegalArgumentException("error executing command " + command, e);
            }
        } else {
            f.mkdirs();
        }
    }

}
