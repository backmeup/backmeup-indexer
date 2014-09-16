package org.backmeup.index;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.backmeup.index.config.Configuration;
import org.backmeup.index.utils.tokenreader.MapTokenResolver;
import org.backmeup.index.utils.tokenreader.TokenReplaceReader;

public class ESConfigurationHandler {

	/**
	 * The elasticsearch_template.yml file contains tokens for http and tcp port
	 * configuration which need to be replaced with the individual user specific
	 * configuration -> which gets written to disc. The data and log file
	 * directory (written in the config file) is either a standard working dir
	 * or a mounted truecrypt container (if one is available)
	 * 
	 * @param tcpport
	 *            accepted port range from 9300 to 9399
	 * @param httpport
	 *            accepted port range from 9200 to 9299
	 * @returns file pointer to the created configuration file
	 */
	public static File createUserYMLStartupFile(int userID, int tcpport,
			int httpport) throws IOException, ExceptionInInitializerError,
			NumberFormatException {

		checkPortRangeAccepted(tcpport, httpport);

		// the tokens within the elasticsearch_template.yml file to replace
		Map<String, String> tokens = new HashMap<>();
		tokens.put("tcpport", tcpport + "");
		tokens.put("httpport", httpport + "");
		tokens.put("clustername", "user" + userID);
		// check if a Troucrypt Volume has been mounted, if not use the default
		// working dir path
		if (IndexManager.getInstance().getTCMountedVolume(userID) != null) {
			tokens.put("pathtologs", IndexManager.getInstance()
					.getTCMountedVolume(userID) + "/index/index-logs");
			tokens.put("pathtodata", IndexManager.getInstance()
					.getTCMountedVolume(userID) + "/index/index-data");
		} else {
			tokens.put("pathtologs", IndexManager.getUserDataWorkingDir(userID)
					+ "/index/index-logs");
			tokens.put("pathtodata", IndexManager.getUserDataWorkingDir(userID)
					+ "/index/index-data");
		}

		MapTokenResolver resolver = new MapTokenResolver(tokens);

		Reader inputReader = new FileReader(new File(
				"src/main/resources/elasticsearch_template.yml"));
		Reader tokenReplaceReader = new TokenReplaceReader(inputReader,
				resolver);
		String outputFile = IndexManager.getUserDataWorkingDir(userID)
				+ "/index/elasticsearch.config.user" + userID + ".yml";
		File file = new File(outputFile);
		file.getParentFile().mkdirs();

		Writer outputStream = new FileWriter(file);
		try {
			int c;
			while ((c = tokenReplaceReader.read()) != -1) {
				outputStream.write(c);
			}
		} finally {
			if (tokenReplaceReader != null) {
				tokenReplaceReader.close();
			}
			if (inputReader != null) {
				inputReader.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}
		return file;

	}

	/**
	 * Starts an elastic search instance for an existing configuration
	 */
	public static void startElasticSearch(int userID) throws IOException,
			InterruptedException {
		String command = "\"" + getElasticSearchExecutable() + "\"" + " "
				+ "-Des.config=" + IndexManager.getUserDataWorkingDir(userID)
				+ "/index/elasticsearch.config.user" + userID + ".yml";
		System.out.println(command);

		try {
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();

		} catch (IOException | InterruptedException e) {
			System.out.println("Error executing: " + command + " "
					+ e.toString());
			throw e;
		}

		// TODO add test if instance is running or check for errors on the
		// console
	}

	public static void stopElasticSearch(int userID) {
		// TODO need to implement. get Process and call quit.
	}

	public static String getElasticSearchExecutable()
			throws ExceptionInInitializerError {
		String s = Configuration.getProperty("elasticsearch.home.dir");
		if (s != null && s.length() > 0 && !s.contains("\"")) {
			File f = new File(s);
			if (f.isDirectory() && f.exists()) {
				String tcexe = f.getAbsolutePath() + "/bin/elasticsearch.bat";
				File tc = new File(tcexe);
				if (tc.isFile() && tc.exists()) {
					return tc.getAbsolutePath();
				}
			}
		}
		throw new ExceptionInInitializerError("Error finding ElasticSearch in "
				+ s);
	}

	public static boolean checkElasticSearchAvailable() {
		String s = Configuration.getProperty("elasticsearch.home.dir");
		if (s != null && s.length() > 0 && !s.contains("\"")) {
			File f = new File(s);
			if (f.isDirectory() && f.exists()) {
				String tcexe = f.getAbsolutePath() + "/bin/elasticsearch.bat";
				File tc = new File(tcexe);
				if (tc.isFile() && tc.exists()) {
					return true;
				}
			}
		}
		return false;
	}

	private static void checkPortRangeAccepted(int tcpport, int httpport)
			throws NumberFormatException {
		if (tcpport < 9300 || tcpport > 9399) {
			throw new NumberFormatException("Provided ElasticSearch tcpport "
					+ tcpport + " is out of accepted range 9300-9399.");
		}
		if (httpport < 9200 || httpport > 9299) {
			throw new NumberFormatException("Provided ElasticSearch httpport "
					+ httpport + " is out of accepted range 9200-9299.");
		}
	}

	public static String getESHomePath() throws ExceptionInInitializerError {
		String s = Configuration.getProperty("elasticsearch.home.dir");

		if (s != null && s.length() > 0 && !s.contains("\"")) {
			File f = new File(s);
			if (f.isDirectory() && f.exists()) {
				return f.getAbsolutePath();
			} else {
				throw new ExceptionInInitializerError(
						"ElasticSearch home.dir does not exist or is not accessible to system");
			}
		}
		throw new ExceptionInInitializerError(
				"ElasticSearch Home not properly configured within backmeup-indexer.properties");
	}

	/**
	 * Returns a pseudo-random number between min and max, inclusive.
	 * 
	 * @param min
	 *            Minimum value
	 * @param max
	 *            Maximum value. Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	private static int randInt(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

}
