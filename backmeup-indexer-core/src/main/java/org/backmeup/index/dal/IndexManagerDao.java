package org.backmeup.index.dal;

import java.net.URL;
import java.util.List;

import org.backmeup.index.core.model.RunningIndexUserConfig;

/**
 * The IndexManagerDao contains all database relevant operations for the model
 * class ESRunningInstanceConfig and TCRunningInstanceConfig. It is used to
 * fetch the persistent information regarding an running index configuration
 * from DB this contains ES (Elastic Search) and TC (True Crypt) port and drive
 * to user ID mappings
 */
public interface IndexManagerDao extends BaseDao<RunningIndexUserConfig> {

	RunningIndexUserConfig findConfigByUserId(Long userID);

	List<RunningIndexUserConfig> getAllESInstanceConfigs();

	/**
	 * Filters the list of running index data from a certain domain
	 * 
	 * @param url
	 *            url must contain protocol and host, no port configuration
	 */
	List<RunningIndexUserConfig> getAllESInstanceConfigs(URL url);

	/**
	 * e.g. URL host = new URL("http", "localhost", 9999, "");
	 * 
	 * @param url
	 *            url must contain protocol, host and httpPort
	 */
	RunningIndexUserConfig findConfigByHttpPort(URL url);

	RunningIndexUserConfig findConfigByClusterName(String clustername);

	RunningIndexUserConfig findConfigByDriveLetter(String driveLetter);

}