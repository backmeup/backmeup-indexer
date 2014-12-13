package org.backmeup.index.dal;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.backmeup.index.dal.jpa.RunningIndexUserConfigDaoImpl;
import org.backmeup.index.dal.jpa.JPAEntityManagerFactoryProducer;
import org.junit.rules.ExternalResource;
import org.mockito.internal.util.reflection.Whitebox;

public class DerbyDatabase extends ExternalResource {

    private EntityManagerFactory entityManagerFactory;
    public EntityManager entityManager;
    public RunningIndexUserConfigDao indexManagerDao;

    @Override
    protected void before() {
        this.entityManagerFactory = new JPAEntityManagerFactoryProducer(overwrittenJPAProps()).create();
        this.entityManager = this.entityManagerFactory.createEntityManager();

        indexManagerDao = new RunningIndexUserConfigDaoImpl();
        Whitebox.setInternalState(indexManagerDao, "entityManager", entityManager);
    }

    private Properties overwrittenJPAProps() {
        Properties overwrittenJPAProps = new Properties();

        overwrittenJPAProps.setProperty("javax.persistence.jdbc.driver", "org.apache.derby.jdbc.EmbeddedDriver");
        overwrittenJPAProps.setProperty("javax.persistence.jdbc.url", "jdbc:derby:target/junit;create=true");

        overwrittenJPAProps.setProperty("hibernate.dialect", "org.hibernate.dialect.DerbyTenSevenDialect");
        overwrittenJPAProps.setProperty("hibernate.hbm2ddl.auto", "create");

        return overwrittenJPAProps;
    }

    @Override
    protected void after() {
        this.entityManager.close();
        new JPAEntityManagerFactoryProducer().destroy(this.entityManagerFactory);
    }

}
