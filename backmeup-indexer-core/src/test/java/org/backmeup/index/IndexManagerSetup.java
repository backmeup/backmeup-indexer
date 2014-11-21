package org.backmeup.index;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

public class IndexManagerSetup {

    public EntityManagerFactory entityManagerFactory;
    public EntityManager entityManager;
    public IndexManager indexManager;

    @After
    public void after() {
        IndexManager.getInstance().shutdownInstance(999991);
        IndexManager.getInstance().shutdownInstance(999992);
        closeEntityManager();
    }

    @Before
    public void before() {
        createEntityManager();
        this.indexManager = IndexManager.getInstance();
        this.indexManager.setEntityManager(this.entityManager);
    }

    @AfterClass
    public static void cleanup() {
        //done automatically within tomcat, neet to call manually within unittests
        IndexManager.getInstance().shutdownIndexManager();
    }

    public void createEntityManager() {
        this.entityManagerFactory = Persistence.createEntityManagerFactory("org.backmeup.index.jpa",
                overwrittenJPAProps());
        this.entityManager = this.entityManagerFactory.createEntityManager();
    }

    public Properties overwrittenJPAProps() {
        Properties overwrittenJPAProps = new Properties();

        overwrittenJPAProps.setProperty("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
        overwrittenJPAProps.setProperty("hibernate.connection.driver_class", "org.apache.derby.jdbc.EmbeddedDriver");
        overwrittenJPAProps.setProperty("javax.persistence.jdbc.url", "jdbc:derby:target/junit;create=true");
        overwrittenJPAProps.setProperty("hibernate.connection.url", "jdbc:derby:target/junit;create=true");
        overwrittenJPAProps.setProperty("hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
        overwrittenJPAProps.setProperty("hibernate.hbm2ddl.auto", "create");

        return overwrittenJPAProps;
    }

    private void closeEntityManager() {
        this.entityManager.close();
        this.entityManagerFactory.close();
    }

}
