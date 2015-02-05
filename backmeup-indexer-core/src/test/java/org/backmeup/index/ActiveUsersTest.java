package org.backmeup.index;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.backmeup.index.core.elasticsearch.SearchInstanceException;
import org.backmeup.index.core.model.RunningIndexUserConfig;
import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.model.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

public class ActiveUsersTest {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();
    protected RunningIndexUserConfigDao dao;
    ActiveUsers activeUsers;

    private User testUser1 = new User(1L);
    private User testUser2 = new User(2L);
    private User testUser3 = new User(3L);

    @Before
    public void before() {
        getDaoFromDb();
        createActiveUsers();
    }

    public void getDaoFromDb() {
        this.dao = this.database.indexManagerDao;
    }

    private void createActiveUsers() {
        this.activeUsers = new ActiveUsers();
        Whitebox.setInternalState(this.activeUsers, "dao", this.dao);
    }

    @Test
    public void testGetActiveUsers() throws IOException {
        List<User> active = this.activeUsers.getActiveUsers();
        Assert.assertTrue(active.size() == 0);
        Assert.assertFalse(this.activeUsers.isUserActive(this.testUser1));

        //now when an ES instance was powered up this results in a db record like this
        markUserAsActiveInDB(this.testUser1);

        //now recheck the activeUsers
        active = this.activeUsers.getActiveUsers();
        Assert.assertTrue(active.size() == 1);
        Assert.assertTrue(active.contains(this.testUser1));
        Assert.assertTrue(this.activeUsers.isUserActive(this.testUser1));

        markUserAsActiveInDB(this.testUser2);
        markUserAsActiveInDB(this.testUser3);
        markUserAsInactiveInDB(this.testUser1);

        active = this.activeUsers.getActiveUsers();
        Assert.assertTrue(active.size() == 2);
        Assert.assertFalse(active.contains(this.testUser1));
        Assert.assertFalse(this.activeUsers.isUserActive(this.testUser1));

    }

    private void markUserAsActiveInDB(User user) {
        RunningIndexUserConfig config = createTestIndexUserConfig(user);
        EntityTransaction tx = this.database.entityManager.getTransaction();
        tx.begin();
        config = this.dao.save(config);
        tx.commit();
    }

    private void markUserAsInactiveInDB(User user) {
        RunningIndexUserConfig config = this.dao.findById(user.id());
        EntityTransaction tx = this.database.entityManager.getTransaction();
        tx.begin();
        this.dao.delete(config);
        tx.commit();
    }

    private RunningIndexUserConfig createTestIndexUserConfig(User userID) {
        try {
            int tcpPort = 20;
            int httpPort = 30;
            String mountedDrive = "/media/test" + userID;

            URI uri = new URI("http", InetAddress.getLocalHost().getHostAddress() + "", "", "");
            return new RunningIndexUserConfig(userID, uri.toURL(), tcpPort, httpPort, "user" + userID, mountedDrive,
                    "/TCContainer/Path");

        } catch (URISyntaxException | UnknownHostException | MalformedURLException e1) {
            throw new SearchInstanceException("creating database object failed");
        }
    }
}
