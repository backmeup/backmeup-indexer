package org.backmeup.index;

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

public class IndexManagerSetup {
    
    @Rule
    public final DerbyDatabase database = new DerbyDatabase();
    
    protected IndexManager indexManager; 
    protected RunningIndexUserConfigDao dao; 

    @After
    public void after() {
        try {
            indexManager.shutdownInstance(new User(999991L));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            indexManager.shutdownInstance(new User(999992L));
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
        this.dao = database.indexManagerDao;
    }

    private void createIndexManager() {
        this.indexManager = new IndexManager();
        Whitebox.setInternalState(indexManager, "dao", dao);
        Whitebox.setInternalState(indexManager, "es", new ES());
        Whitebox.setInternalState(indexManager, "dataContainer", new UserDataStorage());
        Whitebox.setInternalState(indexManager, "encryptionProvider", new EncryptionProvider());
        Whitebox.setInternalState(indexManager, "searchInstance", new SearchInstances());
        Whitebox.setInternalState(indexManager, "indexKeepAliveTimer", new IndexKeepAliveTimer());
    }

}
