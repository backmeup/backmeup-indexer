package org.backmeup.index.dal;

import java.util.List;

import org.backmeup.index.db.RunningIndexUserConfig;

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

	RunningIndexUserConfig findConfigByHttpPort(int httpPort);

	RunningIndexUserConfig findConfigByClusterName(String clustername);

	RunningIndexUserConfig findConfigByDriveLetter(String driveLetter);

}