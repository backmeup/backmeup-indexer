package org.backmeup.index.storage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.backmeup.index.api.IndexFields;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.User;
import org.backmeup.index.serializer.Json;
import org.backmeup.keyserver.fileencryption.EncryptionInputStream;
import org.backmeup.keyserver.fileencryption.EncryptionOutputStream;

/**
 * Implementation of the indexer related Themis data store for file operations for persisting and fetching the Truecrypt
 * container files on/from disk and storing the index-fragment files to public encrypted user space before they get
 * imported on next user login
 * 
 */
public class ThemisDataSink {

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
    // new FileInputStream(f) is closed inside FileUtils.copyInputStreamToFile
    public static void saveIndexTrueCryptContainer(File f, User user) throws IOException {
        if (f == null) {
            throw new IOException("file f is null");
        }

        if (user.id() > -1 && (f.exists() && f.canRead())) {
            saveIndexTrueCryptContainer(new FileInputStream(f), user);
        } else {
            throw new IOException("Error storing Index TrueCrypt Container file " + f.getAbsolutePath() + ": userID: " + user.id()
                    + ", file exists? " + f.exists() + ", file is readable? " + f.canRead());
        }
    }

    public static void saveIndexTrueCryptContainer(InputStream in, User user) throws IOException {
        if (in == null) {
            throw new IOException("file is null");
        }
        File f = new File(getDataSinkHome(user) + "/index/elasticsearch_userdata_TC_150MB.tc");
        if (!f.getParentFile().exists()) {
            mkDirs(f.getParentFile());
        }
        FileUtils.copyInputStreamToFile(in, f);
    }

    /**
     * Removes the user specific TrueCrypt volume from the users file space
     */
    public static void deleteIndexTrueCryptContainer(User user) throws IOException {
        File f = getIndexTrueCryptContainer(user);
        f.delete();
    }

    private static UUID saveIndexFragment(IndexDocument indexFragment, User user, IndexFragmentType type, boolean encrypt,
            PublicKey userPubKey) throws IOException {
        if (encrypt && (userPubKey == null)) {
            throw new IOException("encryption requires publicKey for user");
        }
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
            File f = new File(getDataSinkHome(user) + "/dropoffzone/" + type.getStorageLocation() + uuid + ".serindexdocument");
            if (!f.getParentFile().exists()) {
                mkDirs(f.getParentFile());
            }

            //check if we're persisting encrypted or not
            if (encrypt) {
                persistAndEncryptIndexFragment(serializedIndexDoc, f, user.id(), userPubKey);
            } else {
                //persist indexfragment as non encrypted file on disk
                FileUtils.writeStringToFile(f, serializedIndexDoc);
            }
            return uuid;

        }
        throw new IOException("Error persisting serialized IndexDocument in user space" + getDataSinkHome(user) + "/user" + user.id()
                + "/dropoffzone/" + type.getStorageLocation() + uuid + ".serindexdocument" + " for userID: " + user.id());

    }

    /**
     * Mainly for testing ThemisDataSink for JUnit tests without encryption
     */
    @Deprecated
    protected static UUID saveIndexFragment(IndexDocument indexFragment, User user, IndexFragmentType type) throws IOException {
        return saveIndexFragment(indexFragment, user, type, false, null);
    }

    /**
     * Persists an IndexDocument within the user's public dropoffzone for index fragments in the DataSink. Distinguishes
     * IndexDocuments that are user owned and shared by other users. IndexFragments are encrypted with the public key of
     * the according user (owner or sharing partner)
     */
    public static UUID saveIndexFragment(IndexDocument indexFragment, User user, IndexFragmentType type, PublicKey userPubKey)
            throws IOException {
        return saveIndexFragment(indexFragment, user, type, true, userPubKey);
    }

    /**
     * Takes a serealized index fragment and writes it to the destFile on disk using public key encryption
     */
    private static File persistAndEncryptIndexFragment(String serializedIndexDoc, File destFile, Long userId, PublicKey pubkey)
            throws IOException {
        EncryptionOutputStream out = new EncryptionOutputStream(destFile, userId + "", pubkey); // file ist ein File-Objekt oder es geht auch ein String mit dem Pfad
        try {
            // EncryptedOutputStream ist wie ein "normaler" FileOutputStream zu benutzen
            out.write(serializedIndexDoc.getBytes("UTF-8"));
            out.close();
            return destFile;
        } catch (Exception e) {
            throw new IOException("exception persisting serealized index fragment (publically encrypted) to ThemisDataSink " + e.toString());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
     * Reads a index fragment (publically encrypted) from disk, decrypts it and hands back the the serealized index
     * fragment as String
     */
    private static String readAndDecryptIndexFragment(File f, Long userId, PrivateKey userPrivateKey) throws IOException {
        EncryptionInputStream ein = new EncryptionInputStream(f, userId + "", userPrivateKey);
        try {
            byte[] block = new byte[8];
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int read = 0;
            while ((read = ein.read(block)) != -1) {
                buffer.write(block, 0, read);
            }
            ein.close();
            return buffer.toString("UTF-8");
        } catch (Exception e) {
            throw new IOException("exception reading and decrypting serealized index fragment (publically encrypted) from ThemisDataSink "
                    + e.toString());
        } finally {
            if (ein != null) {
                try {
                    ein.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
     * Only for JUnit testing without encryption
     */
    @Deprecated
    protected static IndexDocument getIndexFragment(UUID objectID, User user, IndexFragmentType type) throws IOException {
        return getIndexFragment(objectID, user, type, false, null);
    }

    public static IndexDocument getIndexFragment(UUID objectID, User user, IndexFragmentType type, PrivateKey userPrivateKey)
            throws IOException {
        return getIndexFragment(objectID, user, type, true, userPrivateKey);
    }

    private static IndexDocument getIndexFragment(UUID objectID, User user, IndexFragmentType type, boolean encrypted,
            PrivateKey userPrivateKey) throws IOException {
        File f = getIndexFragmentFile(objectID, user, type);

        if (user.id() > -1 && (f.exists() && f.canRead())) {
            String serIndexDoc;
            if (encrypted) {
                if (userPrivateKey == null) {
                    throw new IOException("publickey of user " + user.id() + " required for getting serealized indexdocument "
                            + objectID.toString());
                }
                //deal with the encrypted file
                serIndexDoc = readAndDecryptIndexFragment(f, user.id(), userPrivateKey);
            } else {
                //only if encryption is turned off
                List<String> lines = FileUtils.readLines(f, "UTF-8");
                serIndexDoc = "";
                for (String l : lines) {
                    serIndexDoc += l;
                }
            }
            // deserialize the object
            IndexDocument indexDoc = Json.deserialize(serIndexDoc, IndexDocument.class);
            return indexDoc;
        }
        throw new IOException("Error getting index fragment: " + getDataSinkHome(user) + "/dropoffzone/" + type.getStorageLocation()
                + objectID + ".serindexdocument" + ", file exists? " + f.exists() + ", file is readable? " + f.canRead());
    }

    private static File getIndexFragmentFile(UUID objectID, User user, IndexFragmentType type) {
        return new File(getDataSinkHome(user) + "/dropoffzone/" + type.getStorageLocation() + objectID + ".serindexdocument");
    }

    /**
     * Returns a list of all index-fragment UUIDs the user currently has stored within his data source repository
     */
    public static List<UUID> getAllIndexFragmentUUIDs(User user, IndexFragmentType type) {
        List<UUID> ret = new ArrayList<>();
        File f = new File(getDataSinkHome(user) + "/dropoffzone/" + type.getStorageLocation());
        if (!f.exists()) {
            mkDirs(f);
        }

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
            throw new IOException("error deleting fragment: " + getDataSinkHome(user) + "/dropoffzone/" + type.getStorageLocation()
                    + objectID + ".serindexdocument" + ", file exists? " + f.exists() + ", file is readable? " + f.canRead());
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
        String s = getDataSinkHome() + "/user" + user.id();
        File f = new File(s);
        if (f.isDirectory() && f.exists()) {
            return f.getAbsolutePath();
        }
        mkDirs(f);
        if (f.isDirectory() && f.exists()) {
            return f.getAbsolutePath();
        }
        throw new IllegalArgumentException("user home dir does not exist or is not accessible to system " + f.getAbsolutePath());
    }

    /**
     * Returns the root directory for the Themis data storage used by index-core
     */
    private static String getDataSinkHome() {
        String s = Configuration.getProperty("themis-datasink.home.dir");
        if (s != null && s.length() > 0 && !s.contains("\"")) {
            File f = new File(s);
            if (f.isDirectory() && f.exists()) {
                return f.getAbsolutePath();
            }
            mkDirs(f);
            if (f.isDirectory() && f.exists()) {
                return f.getAbsolutePath();
            }
            throw new IllegalArgumentException("datasink home dir does not exist or is not accessible to system " + f.getAbsolutePath());
        }
        throw new IllegalArgumentException("datasink home dir not properly configured within backmeup-indexer.properties");
    }

    private static void mkDirs(File f) {
        /*if (SystemUtils.IS_OS_LINUX) {
            String command = "sudo mkdir -p " + f.getAbsolutePath();
            try {
                int exitVal = CommandLineUtils.executeCommandLine(command, 2, TimeUnit.SECONDS);
                if (exitVal != 0) {
                    throw new IllegalArgumentException("error executing command " + command + " exit value: " + exitVal);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("error executing command " + command, e);
            }
        }*/
        f.mkdirs();
    }

}
