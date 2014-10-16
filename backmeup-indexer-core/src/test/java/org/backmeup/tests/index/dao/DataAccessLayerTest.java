package org.backmeup.tests.index.dao;

import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.backmeup.index.dal.DataAccessLayer;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.dal.jpa.DataAccessLayerImpl;
import org.backmeup.index.db.RunningIndexUserConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the jpa hibernate storage and retrieval layer for index user
 * configurations.
 */
public class DataAccessLayerTest {

	private EntityManagerFactory entityManagerFactory;
	private DataAccessLayer dal;

	@Before
	public void createEntityManager() {
		this.entityManagerFactory = Persistence.createEntityManagerFactory(
				"org.backmeup.index.jpa", overwrittenJPAProps());

		this.dal = new DataAccessLayerImpl();
		this.dal.setEntityManager(this.entityManagerFactory
				.createEntityManager());
	}

	private Properties overwrittenJPAProps() {
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

		return overwrittenJPAProps;
	}

	@After
	public void closeEntityManager() {
		this.entityManagerFactory.close();
	}

	@Test
	public void shouldStorestoreConfigurationAndReadFromDB() {
		RunningIndexUserConfig config = createConfig();

		IndexManagerDao im = this.dal.createIndexManagerDao();
		im.save(config);

		// now check if we can read this information from DB
		RunningIndexUserConfig found = im.findConfigByUserId(77L);
		assertNotNull("config with UserId 77", found);
	}

	private RunningIndexUserConfig createConfig() {
		RunningIndexUserConfig config = new RunningIndexUserConfig();
		config.setHttpPort(9999);
		config.setTcpPort(8888);
		config.setClusterName("testname");
		config.setMountedDriveLetter("/etc/home");
		config.setUserID(Long.valueOf(77));
		return config;
	}

}
