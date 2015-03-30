package org.backmeup.index;

import org.backmeup.index.core.datacontainer.UserDataStorage;
import org.backmeup.index.core.elasticsearch.SearchInstances;
import org.backmeup.index.core.truecrypt.EncryptionProvider;
import org.backmeup.index.dal.DerbyDatabase;
import org.backmeup.index.dal.IndexFragmentEntryStatusDao;
import org.backmeup.index.dal.RunningIndexUserConfigDao;
import org.backmeup.index.dal.SharingPolicyDao;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ES;
import org.backmeup.index.sharing.execution.IndexContentManager;
import org.backmeup.index.sharing.policy.SharingPolicyManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.internal.util.reflection.Whitebox;

public class IndexManagerIntegrationTestSetup {

    @Rule
    public final DerbyDatabase database = new DerbyDatabase();

    protected IndexManager indexManager;
    protected RunningIndexUserConfigDao runningInstancesdao;
    protected IndexContentManager contentManager;
    protected IndexFragmentEntryStatusDao contentStatusDao;
    protected SharingPolicyDao sharingPoliyDao;
    protected SharingPolicyManager sharingPolicyManager;

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
        createIndexContentManager();
        createSharingPolicyManager();
    }

    public void getDaoFromDb() {
        this.runningInstancesdao = this.database.indexManagerDao;
        this.contentStatusDao = this.database.statusDao;
        this.sharingPoliyDao = this.database.sharingPolicyDao;
    }

    private void createIndexManager() {
        this.indexManager = new IndexManager();
        SearchInstances searchInstance = new SearchInstances();
        searchInstance.initAvailableInstances();
        Whitebox.setInternalState(this.indexManager, "dao", this.runningInstancesdao);
        Whitebox.setInternalState(this.indexManager, "es", new ES());
        Whitebox.setInternalState(this.indexManager, "dataContainer", new UserDataStorage());
        Whitebox.setInternalState(this.indexManager, "encryptionProvider", new EncryptionProvider());
        Whitebox.setInternalState(this.indexManager, "searchInstance", searchInstance);
        Whitebox.setInternalState(this.indexManager, "indexKeepAliveTimer", new IndexKeepAliveTimer());
    }

    private void createIndexContentManager() {
        this.contentManager = new IndexContentManager();
        Whitebox.setInternalState(this.contentManager, "entryStatusDao", this.contentStatusDao);
        Whitebox.setInternalState(this.contentManager, "runninInstancesDao", this.runningInstancesdao);
        Whitebox.setInternalState(this.contentManager, "indexManager", this.indexManager);
    }

    private void createSharingPolicyManager() {
        this.sharingPolicyManager = new SharingPolicyManager();
        Whitebox.setInternalState(this.sharingPolicyManager, "sharingPolicyDao", this.sharingPoliyDao);
    }

}
