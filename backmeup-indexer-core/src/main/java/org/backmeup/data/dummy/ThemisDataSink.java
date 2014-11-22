package org.backmeup.data.dummy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.index.model.IndexFields;
import org.backmeup.index.utils.file.JsonSerializer;

/**
 * dummy implementation of a themis data sink currently with file operations for
 * Truecrypt container files required to persist the index data
 * 
 * @TODO add encryption for serialized IndexDocuments on disc
 * 
 */
public class ThemisDataSink {

	public enum IndexFragmentType {
		TO_IMPORT_USER_OWNED("to-import/userowned/"), IMPORTED_USER_OWNED(
				"imported/userowned/");

		private String storage_location;

		IndexFragmentType(String location) {
			this.storage_location = location;
		}

		private String getStorageLocation() {
			return this.storage_location;
		}
	}

	/**
	 * Fetches the user specific TrueCrypt container file which is stored within
	 * the user space
	 */
	public static File getIndexTrueCryptContainer(int userID)
			throws IOException {
		String s = getDataSinkHome(userID) + "/user" + userID
				+ "/index/elasticsearch_userdata_TC_150MB.tc";
		File f = new File(s);
		if (f.exists() && f.canRead()) {
			return f;
		}
		throw new IOException("Truecrypt Data Container for user " + userID
				+ " not found");
	}

	/**
	 * Takes a truecrypt container file containing the Elasticsearch index data
	 * and persists configuration file within the it within the users personal
	 * file space
	 * 
	 * @param f
	 *            the user specific yml ES startup file
	 */
	public static void saveIndexTrueCryptContainer(File f, int userID)
			throws IOException {
		if (f == null) {
			throw new IOException("file f is null");
		}

		if (userID > -1 && (f.exists() && f.canRead())) {
		    saveIndexTrueCryptContainer(new FileInputStream(f), userID);
		} else {
			throw new IOException(
					"Error storing Index TrueCrypt Container file "
							+ f.getAbsolutePath() + ": userID: " + userID
							+ ", file exists? " + f.exists()
							+ ", file is readable? " + f.canRead());
		}
	}

    public static void saveIndexTrueCryptContainer(InputStream in, int userID) throws IOException {
        if (in == null) {
            throw new IOException("file is null");
        }

        FileUtils.copyInputStreamToFile(in, new File(getDataSinkHome(userID) + "/user"
                + userID + "/index/elasticsearch_userdata_TC_150MB.tc"));
    }
	
	/**
	 * Removes the user specific TrueCrypt volume from the users file space
	 */
	public static void deleteIndexTrueCryptContainer(int userID)
			throws IOException {

		File f = getIndexTrueCryptContainer(userID);
		f.delete();
	}

	/**
	 * Persists a shared IndexDocument from userA with userB
	 */
	public static void saveSharedIndexFragment(int userIDShares,
			int userIDToShareWith, UUID indexFragmentRef) throws IOException {
		// TODO OR REMOVE
	}

	/**
	 * Persists an IndexDocument within the user's index-fragments space in the
	 * DataSink. Distinguishes between imported, shared or already indexed
	 * IndexDocuments
	 */
	public static UUID saveIndexFragment(IndexDocument indexFragment,
			int userID, IndexFragmentType type) throws IOException {

		if (userID <= -1) {
			throw new IOException("userID missing");
		}
		if (indexFragment == null) {
			throw new IOException("IndexDocument may not be null");
		}
		if (type == null) {
			throw new IOException(
					"The IndexFragmentType (location) may not be null");
		}

		// the object's UUID. same objects across multiple users have same UUID
		UUID uuid = null;

		// check if we need to generate a unique file name or if if it's sharing
		if (indexFragment.getFields().containsKey(IndexFields.FIELD_INDEX_UUID)) {
			// existing object, has already assigned a UUID
			uuid = (UUID) indexFragment.getFields().get(
					IndexFields.FIELD_INDEX_UUID);
		} else {
			uuid = UUID.randomUUID();
			// before serializing we add the UUID as element within the object
			indexFragment.field(IndexFields.FIELD_INDEX_UUID, uuid);
		}

		// serialize the IndexDocument to JSON
		String serializedIndexDoc = JsonSerializer.serialize(indexFragment);

		if (serializedIndexDoc != null) {

			FileUtils.writeStringToFile(
					new File(getDataSinkHome(userID) + "/user" + userID
							+ "/index-fragments/" + type.getStorageLocation()
							+ uuid + ".serindexdocument"), serializedIndexDoc);
			return uuid;

		}
        
		throw new IOException(
        		"Error persisting serialized IndexDocument in user space"
        				+ getDataSinkHome(userID) + "/user" + userID
        				+ "/index-fragments/" + type.getStorageLocation()
        				+ uuid + ".serindexdocument" + " for userID: "
        				+ userID);

	}

	public static IndexDocument getIndexFragment(UUID objectID, int userID,
			IndexFragmentType type) throws IOException {
		File f = getIndexFragmentFile(objectID, userID, type);

		if (userID > -1 && (f.exists() && f.canRead())) {
			List<String> lines = FileUtils.readLines(f, "UTF-8");

			String serObject = "";
			for (String l : lines) {
				serObject += l;
			}
			// deserialize the object
			IndexDocument indexDoc = JsonSerializer.deserialize(serObject,
					IndexDocument.class);

			return indexDoc;
		}
		
        throw new IOException("Error getting index fragment: "
        		+ getDataSinkHome(userID) + "/user" + userID
        		+ "/index-fragments/" + type.getStorageLocation()
        		+ objectID + ".serindexdocument" + ", file exists? "
        		+ f.exists() + ", file is readable? " + f.canRead());
	}

	private static File getIndexFragmentFile(UUID objectID, int userID,
			IndexFragmentType type) {
		return new File(getDataSinkHome(userID) + "/user" + userID
				+ "/index-fragments/" + type.getStorageLocation() + objectID
				+ ".serindexdocument");
	}

	/**
	 * Returns a list of all index-fragment UUIDs the user currently has stored
	 * within his data source repository
	 */
	public static List<UUID> getAllIndexFragmentUUIDs(int userID,
			IndexFragmentType type) {
		List<UUID> ret = new ArrayList<>();
		File f = new File(getDataSinkHome(userID) + "/user" + userID
				+ "/index-fragments/" + type.getStorageLocation());

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

	public static void deleteIndexFragment(UUID objectID, int userID,
			IndexFragmentType type) throws IOException {
		File f = getIndexFragmentFile(objectID, userID, type);

		if (userID > -1 && (f.exists() && f.canRead())) {
			f.delete();
		} else {
			throw new IOException("error deleting fragment: "
					+ getDataSinkHome(userID) + "/user" + userID
					+ "/index-fragments/" + type.getStorageLocation()
					+ objectID + ".serindexdocument" + ", file exists? "
					+ f.exists() + ", file is readable? " + f.canRead());
		}
	}

	public static void deleteAllIndexFragments(int userID,
			IndexFragmentType type) throws IOException {
		for (UUID uuid : getAllIndexFragmentUUIDs(userID, type)) {
			deleteIndexFragment(uuid, userID, type);
		}
	}

	/**
	 * Returns the root directory for the Themis Datensenke dummy implementation
	 */
	private static String getDataSinkHome(int userID) {
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
			throw new IllegalArgumentException(
					"user home dir does not exist or is not accessible to system");
		}
		throw new IllegalArgumentException(
				"User Home dir not properly configured within backmeup-indexer.properties");
	}

}
