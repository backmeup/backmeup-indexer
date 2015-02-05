package org.backmeup.index.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.model.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests the JPA Hibernate storage and retrieval layer for index user configurations via derby DB with
 * hibernate.hbm2ddl.auto=create
 */
public class RunningIndexUserConfigDaoTest {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    private RunningIndexUserConfigDao indexManagerDao;

    @Before
    public void getDaoFromDb() {
        this.indexManagerDao = this.database.indexManagerDao;
    }

    @Test
    public void shouldStoreConfigurationAndReadFromDBByHttpPort() throws MalformedURLException {
        RunningIndexUserConfig config = createConfig();
        persistInTransaction(config);

        URL host = new URL("http", "localhost", 9999, "");
        RunningIndexUserConfig found = this.indexManagerDao.findConfigByHttpPort(host);
        assertNotNull("config with port 9999", found);
    }

    @Test
    public void shouldStoreConfigurationAndReadFromDBByUserId() throws MalformedURLException {
        RunningIndexUserConfig config = createConfig();
        persistInTransaction(config);

        RunningIndexUserConfig found = this.indexManagerDao.findConfigByUser(new User(77L));
        assertNotNull(found);
        assertEquals(config.getUserID(), found.getUserID());
    }

    @Test
    public void shouldStoreConfigurationAndReadAllFromDB() throws MalformedURLException {
        RunningIndexUserConfig config = createConfig();
        persistInTransaction(config);

        List<RunningIndexUserConfig> found = this.indexManagerDao.getAllESInstanceConfigs();
        assertNotNull(found);
        assertTrue(found.size() > 0);
        assertEquals(config.getUserID(), found.get(0).getUserID());
    }

    @Test
    public void shouldFilterConfigurationByHostAddress() throws MalformedURLException {
        RunningIndexUserConfig config = createConfig();
        persistInTransaction(config);

        List<RunningIndexUserConfig> found = this.indexManagerDao.getAllESInstanceConfigs(new URL("http://localhost"));
        assertNotNull(found);
        assertTrue(found.size() > 0);
        assertEquals(config.getUserID(), found.get(0).getUserID());
        found = this.indexManagerDao.getAllESInstanceConfigs(new URL("http://localhost2"));
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
        config.setUserID(77L);
        return config;
    }

    private void persistInTransaction(RunningIndexUserConfig config) {
        // need manual transaction in test because transactional interceptor is not installed in tests
        this.database.entityManager.getTransaction().begin();
        this.indexManagerDao.save(config);
        this.database.entityManager.getTransaction().commit();
    }

}
