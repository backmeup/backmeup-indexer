package org.backmeup.index.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.dal.jpa.JPADataAccessLayer;
import org.backmeup.index.dal.jpa.JPAEntityManagerFactoryProducer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the jpa hibernate storage and retrieval layer for index user
 * configurations via derby DB with hibernate.hbm2ddl.auto=create
 */
public class IndexManagerDaoTest {

    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private IndexManagerDao indexManagerDao;

    @Before
    public void createEntityManager() {
        this.entityManagerFactory = new JPAEntityManagerFactoryProducer(overwrittenJPAProps()).create();
        this.entityManager = this.entityManagerFactory.createEntityManager();

        JPADataAccessLayer dal = new JPADataAccessLayer();
        dal.setEntityManager(this.entityManager);

        indexManagerDao = dal.createIndexManagerDao();
    }

    private Properties overwrittenJPAProps() {
        Properties overwrittenJPAProps = new Properties();

        overwrittenJPAProps.setProperty("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
        overwrittenJPAProps.setProperty("javax.persistence.jdbc.url", "jdbc:derby:target/junit;create=true");

        overwrittenJPAProps.setProperty("hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
        overwrittenJPAProps.setProperty("hibernate.hbm2ddl.auto", "create");

        return overwrittenJPAProps;
    }

    @After
    public void closeEntityManager() {
        this.entityManager.close();
        new JPAEntityManagerFactoryProducer().destroy(this.entityManagerFactory);
    }

    @Test
    public void shouldStoreConfigurationAndReadFromDBByHttpPort() throws MalformedURLException {
        RunningIndexUserConfig config = createConfig();
        persistInTransaction(config);

        URL host = new URL("http", "localhost", 9999, "");
        RunningIndexUserConfig found = indexManagerDao.findConfigByHttpPort(host);
        assertNotNull("config with port 9999", found);
    }

    @Test
    public void shouldStoreConfigurationAndReadFromDBByUserId() throws MalformedURLException {
        RunningIndexUserConfig config = createConfig();
        persistInTransaction(config);

        RunningIndexUserConfig found = indexManagerDao.findConfigByUserId(77L);
        assertNotNull(found);
        assertEquals(config.getUserID(), found.getUserID());
    }

    @Test
    public void shouldStoreConfigurationAndReadAllFromDB() throws MalformedURLException {
        RunningIndexUserConfig config = createConfig();
        persistInTransaction(config);

        List<RunningIndexUserConfig> found = indexManagerDao.getAllESInstanceConfigs();
        assertNotNull(found);
        assertTrue(found.size() > 0);
        assertEquals(config.getUserID(), found.get(0).getUserID());
    }

    @Test
    public void shouldFilterConfigurationByHostAddress() throws MalformedURLException {
        RunningIndexUserConfig config = createConfig();
        persistInTransaction(config);

        List<RunningIndexUserConfig> found = indexManagerDao.getAllESInstanceConfigs(new URL("http://localhost"));
        assertNotNull(found);
        assertTrue(found.size() > 0);
        assertEquals(config.getUserID(), found.get(0).getUserID());
        found = indexManagerDao.getAllESInstanceConfigs(new URL("http://localhost2"));
        assertNotNull(found);
        assertTrue(found.size() == 0);
    }

    private RunningIndexUserConfig createConfig() throws MalformedURLException {
        RunningIndexUserConfig config = new RunningIndexUserConfig();
        config.setHostAddress(new URL("http://localhost"));
        config.setHttpPort(9999);
        config.setTcpPort(8888);
        config.setClusterName("testname");
        config.setMountedDriveLetter("/etc/home");
        config.setUserID(Long.valueOf(77));
        return config;
    }

    private void persistInTransaction(RunningIndexUserConfig config) {
        this.entityManager.getTransaction().begin();
        indexManagerDao.save(config);
        this.entityManager.getTransaction().commit();
    }

}
