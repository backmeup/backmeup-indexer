package org.backmeup.index;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.backmeup.data.dummy.ThemisDataSink;
import org.backmeup.index.config.Configuration;
import org.backmeup.index.utils.file.FileUtils;

public class IndexManager {

	public static IndexManager getInstance() {
		return im;
	}

	// Note: @see ESConfigurationHandler.checkPortRangeAccepted - these values
	// are currently hardcoded there
	private List<Integer> availableTCPPorts = new ArrayList<>();
	private List<Integer> usedTCPPorts = new ArrayList<>();
	private List<Integer> availableHttpPorts = new ArrayList<>();
	private List<Integer> usedHttpPorts = new ArrayList<>();
	// keeps a userId to Port and DriveLetter mapping
	private HashMap<Integer, HashMap<String, String>> userPortMapping = new HashMap<>();

	private static IndexManager im = new IndexManager();

	// this class is implemented as singleton
	private IndexManager() {
		// init the available port range on elasticsearch
		// Note: @see ESConfigurationHandler.checkPortRangeAccepted - these
		// values are currently also hardcoded there
		// TODO reset the port range
		for (int i = 9360; i <= 9399; i++) {
			availableTCPPorts.add(i);
		}
		for (int i = 9260; i <= 9299; i++) {
			availableHttpPorts.add(i);
		}

	}

	// TODO private final Logger logger =
	// org.slf4j.LoggerFactory.getLogger(ESUserConfigurationManagement.class);
	// Add logging statements for warnings and errors
	// TODO encrypt ES Webservice Endpoint (certificates or basic
	// authentication

	/**
	 * Runs the ES configuration, index data mounting from TrueCrypt and powers
	 * on a ES instance for a given user.
	 * 
	 * @throws IOException
	 *             when the required artifact files are not properly created
	 * @throws NumberFormatException
	 *             when the available range of supported ports on ES is used up
	 * @throws ExceptionInInitializerError
	 *             when issuing the call to TrueCrypt failed
	 * @throws InterruptedException
	 *             when issuing the call to TrueCrypt failed
	 * @throws IllegalArgumentException
	 *             when the TrueCrypt instance was not configured properly
	 */
	public void startup(int userID) throws IOException, NumberFormatException,
			ExceptionInInitializerError, IllegalArgumentException,
			InterruptedException {
		// mount truecrypt container
		// create user specific ES launch configuration (yml file)
		// start ES instance for user
		// elasticsearch -Des.config="C:\Program
		// Files\elasticsearch-1.2.0\config\elasticsearch.user0.yml"

		// 1) check if user has been initialized
		File fTCContainerOnDataSink = null;
		try {
			fTCContainerOnDataSink = ThemisDataSink
					.getIndexTrueCryptContainer(userID);
		} catch (IOException e) {
			// initialize user
			init(userID);
			// try the call again - user now initialized, if error -> throw
			// exceptions
			fTCContainerOnDataSink = ThemisDataSink
					.getIndexTrueCryptContainer(userID);
		}

		// 2) get a local copy of the TrueCrypt container for the given user
		File fTCContainer = copyTCContainerFileToLocalWorkingDir(
				fTCContainerOnDataSink, userID);

		// 3) Now mount the ES data volume
		// TODO currently when all available drives are in use the system will
		// throw an IOException
		String tcMountedDriveLetter;

		tcMountedDriveLetter = TCMountHandler.mount(fTCContainer, "12345",
				TCMountHandler.getSupportedDriveLetters().get(0));

		System.out.println("Mounted Drive Letter: " + tcMountedDriveLetter);

		// 4) crate a user specific ElasticSearch startup configuration file
		// TODO currently when all available ports are in use the system will
		// throw a NumberFormatException
		int tcpPort = getFreeESTCPPort();
		int httpPort = getFreeESHttpPort();

		// this file contains the user specific port configuration, data and log
		// location, etc.
		File fYML = ESConfigurationHandler.createUserYMLStartupFile(userID,
				tcpPort, httpPort);

		// keep a record of this configuration
		this.setUserPortMapping(userID, httpPort, tcpPort, tcMountedDriveLetter);

		// just of testing:
		System.out.println("using Drive: " + this.getTCMountedVolume(userID));

		// 5) now power on elasticsearch
		ESConfigurationHandler.startElasticSearch(userID);
		System.out.println("started ES Instance? on port: "
				+ getESTHttpPort(userID));

		// check instance up and running
		// import waiting index files (shared data)
	}

	public void shutdown(int userID) throws IllegalArgumentException,
			ExceptionInInitializerError, IOException, InterruptedException {
		// TODO persist the index data files and write back to data store

		// shutdown the ElasticSearch Instance
		ESConfigurationHandler.stopElasticSearch(userID);

		// release the ES ports
		this.releaseESHttpPort(getESTHttpPort(userID));
		this.releaseESTCPPort(getESTcpPort(userID));

		// unmount the truecrypt volume
		String driveLetter = getTCMountedVolume(userID);
		TCMountHandler.unmount(driveLetter);

		// remove the port mapping to user history
		removeUserPortMapping(userID);

		// whipe the data and yml configuration file
		deleteLocalWorkingDir(userID);
	}

	/**
	 * Cleanup - stops all running ES instances, removes all mounted TC
	 * container
	 */
	public void cleanup() throws IOException, InterruptedException {
		// TODO IMPLEMENT
		// unmount all open TrueCrypt volumes
		TCMountHandler.unmountAll();
		// shutdown all elastic search instances
		ESConfigurationHandler.stopAll();
		// TODO delete all working directories
	}

	/**
	 * Keeps a record which ports have been used for the ElasticSearch
	 * configuration and which DriveLetter has been mounted with Truecrypt
	 */
	private void setUserPortMapping(int userID, int httpPort, int tcpPort,
			String driveLetter) {
		HashMap<String, String> m = new HashMap<>();
		m.put("httpPort", httpPort + "");
		m.put("tcpPort", tcpPort + "");
		m.put("tcDriveLetter", driveLetter);
		this.userPortMapping.put(userID, m);
	}

	/**
	 * Removes the record of which ES ports and TrueCrypt volume has been used
	 */
	private void removeUserPortMapping(int userID) {
		if (this.userPortMapping.containsKey(userID)) {
			this.userPortMapping.remove(userID);
		}
	}

	public int getESTcpPort(int userID) {
		HashMap<String, String> m = this.userPortMapping.get(userID);
		if (m != null && m.containsKey("tcpPort")) {
			return Integer.valueOf(m.get("tcpPort"));
		}
		return -1;
	}

	public int getESTHttpPort(int userID) {
		HashMap<String, String> m = this.userPortMapping.get(userID);
		if (m != null && m.containsKey("httpPort")) {
			return Integer.valueOf(m.get("httpPort"));
		}
		return -1;
	}

	/**
	 * Returns the mounted volume's drive letter for a given userID
	 */
	public String getTCMountedVolume(int userID) {
		HashMap<String, String> m = this.userPortMapping.get(userID);

		if (m != null && m.containsKey("tcDriveLetter")) {
			// note the return value can also be null
			return m.get("tcDriveLetter");
		}
		return null;
	}

	private int getFreeESHttpPort() {
		int ret = -1;
		ret = this.availableHttpPorts.get(0);
		this.usedHttpPorts.add(ret);
		this.availableHttpPorts.remove(0);
		return ret;
	}

	private int getFreeESTCPPort() {
		int ret = -1;
		ret = this.availableTCPPorts.get(0);
		this.usedTCPPorts.add(ret);
		this.availableTCPPorts.remove(0);
		return ret;
	}

	private void releaseESHttpPort(int port) {
		int i = this.usedHttpPorts.indexOf(port);
		if (i != -1) {
			this.usedHttpPorts.remove(i);
			this.availableHttpPorts.add(port);
		}
	}

	private void releaseESTCPPort(int port) {
		int i = this.usedTCPPorts.indexOf(port);
		if (i != -1) {
			this.usedTCPPorts.remove(i);
			this.availableTCPPorts.add(port);
		}
	}

	private void releaseTCMountedVolume(int port) {
		int i = this.usedHttpPorts.indexOf(port);
		if (i != -1) {
			this.usedHttpPorts.remove(i);
			this.availableHttpPorts.add(port);
		}
	}

	/**
	 * Inits a user specific elasticsearch instance i.e. copies the container
	 * file and registers it within the themis-datasink
	 */
	public void init(int userID) {
		// TODO fix weakness currently all copied TC container files have the
		// same default password as this cannot be changed via TC command line
		// interface. idea: keep default password but encrypt the container file
		try {
			ThemisDataSink
					.saveIndexTrueCryptContainer(
							new File(
									"src/main/resources/elasticsearch_userdata_template_TC_150MB.tc"),
							userID);
		} catch (IOException e) {
			// TODO add log statement
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * Gets the root directory (for index operations) for all users done.
	 */
	public static String getUserDataWorkingDirRoot() {
		String s = Configuration.getProperty("index.temp.data.home.dir");
		if (s != null && s.length() > 0 && !s.contains("\"")) {
			File f = new File(s);
			if (f.isDirectory() && f.exists()) {
				return f.getAbsolutePath();
			} else {
				f.mkdirs();
				if (f.isDirectory() && f.exists()) {
					return f.getAbsolutePath();
				}
			}
			throw new ExceptionInInitializerError(
					"index.temp.data.home.dir does not exist or is not accessible to system");
		}
		throw new ExceptionInInitializerError(
				"index.temp.data.home.dir not properly configured within backmeup-indexer.properties");
	}

	/**
	 * Gets the user's working directory on the temporary file share to operate
	 * the index upon
	 */
	public static String getUserDataWorkingDir(int userID) {
		return getUserDataWorkingDirRoot() + "/user" + userID;
	}

	private File copyTCContainerFileToLocalWorkingDir(File f, int userID)
			throws IOException {
		return FileUtils.copyFileUsingChannel(f, new File(
				getUserDataWorkingDir(userID)
						+ "/index/elasticsearch_userdata_TC_150MB.tc"));
	}

	/**
	 * Deletes the working directory for a given user including all files within
	 * it
	 */
	private void deleteLocalWorkingDir(int userID) {
		File f = new File(getUserDataWorkingDir(userID));
		if (f.exists()) {
			FileUtils.deleteDirectory(f);
		}

	}

}
