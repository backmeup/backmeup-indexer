package org.backmeup.index;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.backmeup.index.dal.IndexManagerDao;
import org.backmeup.index.model.User;
import org.backmeup.index.query.ES;
import org.junit.After;
import org.junit.Before;

public class IndexManagerSetup {

    protected EntityManagerFactory entityManagerFactory;
    protected EntityManager entityManager;
    protected IndexManager indexManager;
    protected IndexStartup startup; // TODO PK need to inkect and all
    protected IndexShutdown shutdown; // TODO PK need to inkect and all
    protected ES es; // TODO PK need to inkect and all
    protected IndexManagerDao dao; // TODO PK need to inkect and all

    @After
    public void after() {
        shutdown.shutdownInstance(new User(999991L));
        shutdown.shutdownInstance(new User(999992L));
        closeEntityManager();
    }

    @Before
    public void before() {
        createEntityManager();
        createIndexManager();
    }

    public void createEntityManager() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory("org.backmeup.index.jpa", overwrittenJPAProps());
        this.entityManager = this.entityManagerFactory.createEntityManager();
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
        this.indexManager.injectForTests(this.entityManager);
        this.indexManager.startupIndexManager();
    }

    private void closeEntityManager() {
        this.entityManager.close();
        this.entityManagerFactory.close();
    }

}
