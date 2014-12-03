package org.backmeup.index.dal.jpa;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.backmeup.index.dal.DataAccessLayer;
import org.backmeup.index.dal.IndexManagerDao;

/**
 * The DataAccessLayerImpl uses JPA to interact with the underlying database.
 */
@RequestScoped
public class DataAccessLayerImpl implements DataAccessLayer {

    @Inject
    private EntityManager entityManager;

    @Produces
    @RequestScoped
    @Override
    public IndexManagerDao createIndexManagerDao() {
        return new IndexManagerDaoImpl(entityManager);
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }
}
