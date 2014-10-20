package org.backmeup.tests.index.dao;

import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.backmeup.index.dal.DataAccessLayer;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.dal.jpa.DataAccessLayerImpl;
import org.backmeup.index.db.RunningIndexUserConfig;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the jpa hibernate storage and retrieval layer for index user
 * configurations via derby DB with hibernate.hbm2ddl.auto=create
 */
public class DataAccessLayerTest {

	private EntityManagerFactory entityManagerFactory;
	private DataAccessLayer dal;
	private EntityManager entityManager;

	@Before
	public void createEntityManager() {
		this.entityManagerFactory = Persistence.createEntityManagerFactory(
				"org.backmeup.index.jpa", overwrittenJPAProps());

		this.dal = new DataAccessLayerImpl();
		this.entityManager = this.entityManagerFactory.createEntityManager();
		this.dal.setEntityManager(this.entityManager);
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
	public void shouldStorestoreConfigurationAndReadFromDBByHttpPort() {
		RunningIndexUserConfig config = createConfig();

		this.entityManager.getTransaction().begin();
		IndexManagerDao im = this.dal.createIndexManagerDao();
		im.save(config);
		this.entityManager.getTransaction().commit();

		RunningIndexUserConfig found = im.findConfigByHttpPort(9999);
		assertNotNull("config with port 9999", found);
	}

	@Test
	public void shouldStorestoreConfigurationAndReadFromDBByUserId() {
		RunningIndexUserConfig config = createConfig();

		this.entityManager.getTransaction().begin();
		IndexManagerDao im = this.dal.createIndexManagerDao();
		im.save(config);
		this.entityManager.getTransaction().commit();

		RunningIndexUserConfig found = im.findConfigByUserId(77L);
		assertNotNull(found);
		Assert.assertEquals(config.getUserID(), found.getUserID());
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
