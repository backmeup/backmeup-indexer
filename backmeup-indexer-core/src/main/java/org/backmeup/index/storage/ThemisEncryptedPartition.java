package org.backmeup.index.storage;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;

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
    public static UUID saveIndexFragment(IndexDocument indexFragment, User user, IndexFragmentType type,
            String mountedDrive) throws IOException {

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

            FileUtils.writeStringToFile(
                    new File(getIndexFragmentStorageZone(mountedDrive) + "/" + type.getStorageLocation() + uuid
                            + ".serindexdocument"), serializedIndexDoc);
            return uuid;

        }
        throw new IOException("Error persisting serialized IndexDocument in encrypted partition"
                + getIndexFragmentStorageZone(mountedDrive) + "/" + type.getStorageLocation() + uuid
                + ".serindexdocument" + " for userID: " + user.id());
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

}
