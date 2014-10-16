package org.backmeup.tests.index.dao;

import org.backmeup.index.dal.DataAccessLayer;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.dal.jpa.DataAccessLayerImpl;
import org.backmeup.index.db.RunningIndexUserConfig;
import org.junit.Test;

/**
 * Tests the jpa hibernate storage and retrieval layer for index user
 * configurations
 * 
 */
public class IndexUserConfigurationPeristencyTest {

	@Test
	public void createDBEntry() {

	}

	@Test
	public void findByUserID() {

	}

	protected DataAccessLayer dal;

	// this class is implemented as singleton
	public void storeConfigurationAndReadFromDB() {

		DataAccessLayer dal = new DataAccessLayerImpl();
		dal.setConnection(connection);

		// ??which connection??

		RunningIndexUserConfig config = new RunningIndexUserConfig();
		config.setHttpPort(9999);
		config.setTcpPort(8888);
		config.setClusterName("testname");
		config.setMountedDriveLetter("/etc/home");
		config.setUserID(Long.valueOf(77));

		IndexManagerDao im = dal.createIndexManagerDao();
		im.save(config);
		
		//now check if we can read this information from DB

	}

}
