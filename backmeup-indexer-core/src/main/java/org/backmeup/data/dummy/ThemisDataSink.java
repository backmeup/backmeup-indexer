package org.backmeup.data.dummy;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.model.IndexDocument;
import org.backmeup.model.serializer.JsonSerializer;

/**
 * dummy implementation of a themis data sink currently with file operations for
 * Truecrypt container files required to persist the index data
 * 
 * @author LindleyA
 * 
 */
public class ThemisDataSink {

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
			FileUtils.copyFile(f, new File(getDataSinkHome(userID) + "/user"
					+ userID + "/index/elasticsearch_userdata_TC_150MB.tc"));
		} else {
			throw new IOException(
					"Error storing Index TrueCrypt Container file "
							+ f.getAbsolutePath() + ": userID: " + userID
							+ ", file exists? " + f.exists()
							+ ", file is readable? " + f.canRead());
		}
	}

	/**
	 * Removes the user specific TrueCrypt volume from the users file space
	 */
	public static void deleteIndexTrueCryptContainer(int userID)
			throws IOException {

		File f = getIndexTrueCryptContainer(userID);
		f.delete();
	}

	public static void shareIndexFragment(int fromUserID, int withUserID,
			IndexDocument indexFragment) {

	}

	/**
	 * Persists an IndexDocument within the user's index-fragments space in the
	 * DataSink
	 * 
	 * @param indexFragment
	 * @param userID
	 * @return uuid of the object created
	 * @throws IOException
	 */
	public static UUID saveIndexFragment(IndexDocument indexFragment, int userID)
			throws IOException {
		if (indexFragment == null) {
			throw new IOException("IndexDocument may not be null");
		}

		// serialize the IndexDocument to JSON
		String serializedIndexDoc = JsonSerializer.serialize(indexFragment);
		UUID uuid = UUID.randomUUID();

		if (userID > -1 && (serializedIndexDoc != null)) {

			FileUtils.writeStringToFile(new File(getDataSinkHome(userID)
					+ "/user" + userID + "/index-fragments/" + uuid
					+ ".serindexdocument"), serializedIndexDoc);
			return uuid;

		} else {
			throw new IOException(
					"Error persisting serialized IndexDocument in user space"
							+ getDataSinkHome(userID) + "/user" + userID
							+ "/index-fragments/" + uuid + ".serindexdocument"
							+ " for userID: " + userID);
		}
	}

	public static IndexDocument getIndexFragment(UUID objectID, int userID)
			throws IOException {
		File f = getIndexFragmentFile(objectID, userID);

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
		} else {
			throw new IOException("Error getting index fragment: "
					+ getDataSinkHome(userID) + "/user" + userID
					+ "/index-fragments/" + objectID + ".serindexdocument"
					+ ", file exists? " + f.exists() + ", file is readable? "
					+ f.canRead());
		}
	}

	private static File getIndexFragmentFile(UUID objectID, int userID) {
		return new File(getDataSinkHome(userID) + "/user" + userID
				+ "/index-fragments/" + objectID + ".serindexdocument");
	}

	/**
	 * Returns a list of all index-fragment UUIDs the user currently has stored
	 * within his data source repository
	 * 
	 * @param userID
	 * @return
	 */
	public static List<UUID> getAllIndexFragmentUUIDs(int userID) {
		List<UUID> ret = new ArrayList<UUID>();
		File f = new File(getDataSinkHome(userID) + "/user" + userID
				+ "/index-fragments/");

		FilenameFilter textFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String lowercaseName = name.toLowerCase();
				if (lowercaseName.endsWith(".serindexdocument")) {
					return true;
				} else {
					return false;
				}
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

	public static void deleteIndexFragment(UUID objectID, int userID)
			throws IOException {
		File f = getIndexFragmentFile(objectID, userID);

		if (userID > -1 && (f.exists() && f.canRead())) {
			f.delete();
		} else {
			throw new IOException("error deleting fragment: "
					+ getDataSinkHome(userID) + "/user" + userID
					+ "/index-fragments/" + objectID + ".serindexdocument"
					+ ", file exists? " + f.exists() + ", file is readable? "
					+ f.canRead());
		}
	}

	public static void deleteAllIndexFragments(int userID) throws IOException {
		for (UUID uuid : getAllIndexFragmentUUIDs(userID)) {
			deleteIndexFragment(uuid, userID);
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
			throw new ExceptionInInitializerError(
					"user home dir does not exist or is not accessible to system");
		}
		throw new ExceptionInInitializerError(
				"User Home dir not properly configured within backmeup-indexer.properties");
	}

}
