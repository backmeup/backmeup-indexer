package org.backmeup.index.dal.jpa;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.backmeup.index.dal.IndexManagerDao;

/**
 * The DataAccessLayer provides access to any kind of database. It uses Data
 * Access Objects (e.g. IndexManagerDao) to store, retrieve and delete data of a
 * certain database. It uses JPA to interact with the underlying database.
 */
@RequestScoped
public class JPADataAccessLayer {

    @Inject
    private EntityManager entityManager;

    @Produces
    @RequestScoped
    public IndexManagerDao createIndexManagerDao() {
        return new IndexManagerDaoImpl(entityManager);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
