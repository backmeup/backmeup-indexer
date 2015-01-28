package org.backmeup.index.sharing;

import org.backmeup.index.IndexKeepAliveTimer;
import org.backmeup.index.IndexManager;
import org.backmeup.index.core.datacontainer.UserDataStorage;
import org.backmeup.index.core.elasticsearch.SearchInstances;
import org.backmeup.index.core.truecrypt.EncryptionProvider;
import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ES;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.internal.util.reflection.Whitebox;

public class SharingPolicyManagerSetup {
    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    protected IndexManager indexManager;
    protected RunningIndexUserConfigDao dao;

    @After
    public void after() {
        try {
            this.indexManager.shutdownInstance(new User(999991L));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            this.indexManager.shutdownInstance(new User(999992L));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Before
    public void before() {
        getDaoFromDb();
        createIndexManager();
    }

    public void getDaoFromDb() {
        this.dao = this.database.indexManagerDao;
    }

    private void createIndexManager() {
        this.indexManager = new IndexManager();
        Whitebox.setInternalState(this.indexManager, "dao", this.dao);
        Whitebox.setInternalState(this.indexManager, "es", new ES());
        Whitebox.setInternalState(this.indexManager, "dataContainer", new UserDataStorage());
        Whitebox.setInternalState(this.indexManager, "encryptionProvider", new EncryptionProvider());
        Whitebox.setInternalState(this.indexManager, "searchInstance", new SearchInstances());
        Whitebox.setInternalState(this.indexManager, "indexKeepAliveTimer", new IndexKeepAliveTimer());
    }

}
