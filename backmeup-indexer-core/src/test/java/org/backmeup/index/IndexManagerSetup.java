package org.backmeup.index;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.backmeup.index.core.datacontainer.UserDataStorage;
import org.backmeup.index.core.elasticsearch.SearchInstances;
import org.backmeup.index.core.truecrypt.EncryptionProvider;
import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.dal.jpa.IndexManagerDaoImpl;
import org.backmeup.index.dal.jpa.JPAEntityManagerFactoryProducer;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ES;
import org.junit.After;
import org.junit.Before;
import org.mockito.internal.util.reflection.Whitebox;

public class IndexManagerSetup {
    protected EntityManagerFactory entityManagerFactory;
    protected EntityManager entityManager;
    protected IndexManager indexManager; 
    protected IndexManagerDao dao; 

    @After
    public void after() {
        indexManager.shutdownInstance(new User(999991L));
        indexManager.shutdownInstance(new User(999992L));
        closeEntityManager();
    }
    
    @Before
    public void before() {
        createTestEntityManager();
        createIndexManager();
    }

    public void createTestEntityManager() {
        this.entityManagerFactory = new JPAEntityManagerFactoryProducer(overwrittenJPAProps()).create();
        this.entityManager = this.entityManagerFactory.createEntityManager();
        
        this.dao = new IndexManagerDaoImpl();
        Whitebox.setInternalState(dao, "entityManager", entityManager);
    }

    public Properties overwrittenJPAProps() {
        Properties overwrittenJPAProps = new Properties();

        overwrittenJPAProps.setProperty("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
        overwrittenJPAProps.setProperty("javax.persistence.jdbc.url", "jdbc:derby:target/junit;create=true");

        overwrittenJPAProps.setProperty("hibernate.dialect", "org.hibernate.dialect.DerbyTenSevenDialect");
        overwrittenJPAProps.setProperty("hibernate.hbm2ddl.auto", "create");

        return overwrittenJPAProps;
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

    private void closeEntityManager() {
        this.entityManager.close();
        new JPAEntityManagerFactoryProducer().destroy(this.entityManagerFactory);
    }

}
