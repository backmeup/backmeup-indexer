package org.backmeup.index.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;

/**
 * dummy implementation of a themis data sink currently with file operations for Truecrypt container files required to
 * persist the index data
 * 
 * @TODO add encryption for serialized IndexDocuments on disc
 * 
 */
public class ThemisDataSink {
    // TODO PK make an instance, extract interface of datasinks to have different types available

    public enum IndexFragmentType {
        TO_IMPORT_USER_OWNED("to-import/userowned/"), IMPORTED_USER_OWNED("imported/userowned/"), TO_IMPORT_SHARED_WITH_USER(
                "to-import/sharedwithuser/"), IMPORTED_SHARED_WITH_USER("imported/sharedwithuser/");

        private String storage_location;

        IndexFragmentType(String location) {
            this.storage_location = location;
        }

        private String getStorageLocation() {
            return this.storage_location;
        }
    }

    /**
     * Fetches the user specific TrueCrypt container file which is stored within the user space
     */
    public static File getIndexTrueCryptContainer(User user) throws IOException {
        String s = getDataSinkHome(user) + "/index/elasticsearch_userdata_TC_150MB.tc";
        File f = new File(s);
        if (f.exists() && f.canRead()) {
            return f;
        }
        throw new IOException("Truecrypt Data Container for user " + user.id() + " not found");
    }

    /**
     * Takes a truecrypt container file containing the Elasticsearch index data and persists it within the users
     * personal file space
     * 
     * @param f
     *            the user specific yml ES startup file
     */
    @SuppressWarnings("resource")
    // new FileInputStream(f) is closed inside FileUtils.copyInputStreamToFile
    public static void saveIndexTrueCryptContainer(File f, User user) throws IOException {
        if (f == null) {
            throw new IOException("file f is null");
        }

        if (user.id() > -1 && (f.exists() && f.canRead())) {
            saveIndexTrueCryptContainer(new FileInputStream(f), user);
        } else {
            throw new IOException("Error storing Index TrueCrypt Container file " + f.getAbsolutePath() + ": userID: "
                    + user.id() + ", file exists? " + f.exists() + ", file is readable? " + f.canRead());
        }
    }

    public static void saveIndexTrueCryptContainer(InputStream in, User user) throws IOException {
        if (in == null) {
            throw new IOException("file is null");
        }

        FileUtils.copyInputStreamToFile(in, new File(getDataSinkHome(user)
                + "/index/elasticsearch_userdata_TC_150MB.tc"));
    }

    /**
     * Removes the user specific TrueCrypt volume from the users file space
     */
    public static void deleteIndexTrueCryptContainer(User user) throws IOException {
        File f = getIndexTrueCryptContainer(user);
        f.delete();
    }

    /**
     * Persists an IndexDocument within the user's public dropoffzone for index fragments in the DataSink. Distinguishes
     * IndexDocuments that are user owned and shared by other users
     */
    public static UUID saveIndexFragment(IndexDocument indexFragment, User user, IndexFragmentType type)
            throws IOException {

        if (indexFragment == null) {
            throw new IOException("IndexDocument may not be null");
        }
        if (type == null) {
            throw new IOException("The IndexFragmentType location type may not be null");
        }

        // the object's UUID. same objects across multiple users have same UUID
        UUID uuid = null;

        // check if we need to generate a unique file name or if if it's sharing
        if (indexFragment.getFields().containsKey(IndexFields.FIELD_INDEX_DOCUMENT_UUID)) {
            // existing object, has already assigned a UUID
            String uuidString = (String) indexFragment.getFields().get(IndexFields.FIELD_INDEX_DOCUMENT_UUID);
            uuid = UUID.fromString(uuidString);
        } else {
            throw new IOException("The IndexFragment must have a document UUID assigned");
        }

        // serialize the IndexDocument to JSON
        String serializedIndexDoc = Json.serialize(indexFragment);
        if (serializedIndexDoc != null) {

            FileUtils.writeStringToFile(new File(getDataSinkHome(user) + "/dropoffzone/" + type.getStorageLocation()
                    + uuid + ".serindexdocument"), serializedIndexDoc);
            return uuid;

        }

        throw new IOException("Error persisting serialized IndexDocument in user space" + getDataSinkHome(user)
                + "/user" + user.id() + "/dropoffzone/" + type.getStorageLocation() + uuid + ".serindexdocument"
                + " for userID: " + user.id());

    }

    public static IndexDocument getIndexFragment(UUID objectID, User user, IndexFragmentType type) throws IOException {
        File f = getIndexFragmentFile(objectID, user, type);

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

        throw new IOException("Error getting index fragment: " + getDataSinkHome(user) + "/dropoffzone/"
                + type.getStorageLocation() + objectID + ".serindexdocument" + ", file exists? " + f.exists()
                + ", file is readable? " + f.canRead());
    }

    private static File getIndexFragmentFile(UUID objectID, User user, IndexFragmentType type) {
        return new File(getDataSinkHome(user) + "/dropoffzone/" + type.getStorageLocation() + objectID
                + ".serindexdocument");
    }

    /**
     * Returns a list of all index-fragment UUIDs the user currently has stored within his data source repository
     */
    public static List<UUID> getAllIndexFragmentUUIDs(User user, IndexFragmentType type) {
        List<UUID> ret = new ArrayList<>();
        File f = new File(getDataSinkHome(user) + "/dropoffzone/" + type.getStorageLocation());

        FilenameFilter textFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(".serindexdocument");
            }
        };

        // iterate over all elements matching the filter
        File[] files = f.listFiles(textFilter);
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                String s = files[i].getName();
                if (s.contains(".")) {
                    s = s.substring(0, s.lastIndexOf("."));
                }
                ret.add(UUID.fromString(s));
            }
        }
        return ret;
    }

    public static void deleteIndexFragment(UUID objectID, User user, IndexFragmentType type) throws IOException {
        File f = getIndexFragmentFile(objectID, user, type);

        if (user.id() > -1 && (f.exists() && f.canRead())) {
            f.delete();
        } else {
            throw new IOException("error deleting fragment: " + getDataSinkHome(user) + "/dropoffzone/"
                    + type.getStorageLocation() + objectID + ".serindexdocument" + ", file exists? " + f.exists()
                    + ", file is readable? " + f.canRead());
        }
    }

    public static void deleteAllIndexFragments(User user, IndexFragmentType type) throws IOException {
        for (UUID uuid : getAllIndexFragmentUUIDs(user, type)) {
            deleteIndexFragment(uuid, user, type);
        }
    }

    /**
     * Wipes all information for a given user within the DataSink (not recommended!)
     * 
     * @param userID
     */
    public static void deleteDataSinkHome(User user) {
        try {
            FileUtils.deleteDirectory(new File(getDataSinkHome(user)));
        } catch (IOException e) {
            throw new IllegalArgumentException("user home dir does not exist or is not accessible to system", e);
        }
    }

    private static String getDataSinkHome(User user) {
        return getDataSinkHome() + "/user" + user.id();
    }

    /**
     * Returns the root directory for the Themis datasink implementation used by index-core
     */
    private static String getDataSinkHome() {
        String s = Configuration.getProperty("themis-datasink.home.dir");
        if (s != null && s.length() > 0 && !s.contains("\"")) {
            File f = new File(s);
            if (f.isDirectory() && f.exists()) {
                return f.getAbsolutePath();
            }
            f.mkdirs();
            if (f.isDirectory() && f.exists()) {
                return f.getAbsolutePath();
            }
            throw new IllegalArgumentException("user home dir does not exist or is not accessible to system");
        }
        throw new IllegalArgumentException("User Home dir not properly configured within backmeup-indexer.properties");
    }

}
