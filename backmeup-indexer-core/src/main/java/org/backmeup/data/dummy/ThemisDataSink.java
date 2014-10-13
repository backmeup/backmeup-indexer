package org.backmeup.data.dummy;

import java.io.File;
import java.io.IOException;

import org.backmeup.index.config.Configuration;
import org.backmeup.index.utils.file.FileUtils;

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
			FileUtils.copyFileUsingChannel(f, new File(getDataSinkHome(userID)
					+ "/user" + userID
					+ "/index/elasticsearch_userdata_TC_150MB.tc"));
		} else {
			throw new IOException(
					"Error storing Index TrueCrypt Container file "
							+ f.getAbsolutePath() + ": userID: " 
							+ userID + ", file exists? " + f.exists()
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
