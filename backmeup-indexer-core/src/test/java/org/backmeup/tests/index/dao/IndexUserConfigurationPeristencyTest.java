package org.backmeup.tests.index.dao;

import java.util.Properties;

import javax.persistence.Persistence;

import org.backmeup.index.dal.DataAccessLayer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the jpa hibernate storage and retrieval layer for index user
 * configurations
 */
public class IndexUserConfigurationPeristencyTest {

	@Before
	public void createEntityManager() {
		Properties overwrittenJPAProps = new Properties();
		overwrittenJPAProps.setProperty("javax.persistence.jdbc.driver",
				"org.apache.derby.jdbc.EmbeddedDriver");
		overwrittenJPAProps.setProperty("hibernate.connection.driver_class",
				"org.apache.derby.jdbc.EmbeddedDriver");
		overwrittenJPAProps.setProperty("javax.persistence.jdbc.url",
				"jdbc:derby:target/junit;create=true");
		overwrittenJPAProps.setProperty("hibernate.connection.url",
				"jdbc:derby:target/junit;create=true");

		overwrittenJPAProps.setProperty("hibernate.dialect",
				"org.hibernate.dialect.DerbyDialect");
		overwrittenJPAProps.setProperty("hibernate.hbm2ddl.auto", "create");
		Persistence.createEntityManagerFactory("org.backmeup.index.jpa",
				overwrittenJPAProps);
	}

	protected DataAccessLayer dal;

	// this class is implemented as singleton
	// public void storeConfigurationAndReadFromDB() {
	//
	// DataAccessLayer dal = new DataAccessLayerImpl();
	// dal.setConnection(connection);
	//
	// // ??which connection??
	//
	// RunningIndexUserConfig config = new RunningIndexUserConfig();
	// config.setHttpPort(9999);
	// config.setTcpPort(8888);
	// config.setClusterName("testname");
	// config.setMountedDriveLetter("/etc/home");
	// config.setUserID(Long.valueOf(77));
	//
	// IndexManagerDao im = dal.createIndexManagerDao();
	// im.save(config);
	//
	// // now check if we can read this information from DB
	//
	// }

	@Test
	public void testDummy() {
		Assert.assertTrue(true);
	}

}
