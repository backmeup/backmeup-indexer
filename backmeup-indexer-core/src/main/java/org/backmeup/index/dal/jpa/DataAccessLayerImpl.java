package org.backmeup.index.dal.jpa;

import javax.persistence.EntityManager;

import org.backmeup.index.dal.DataAccessLayer;
import org.backmeup.index.dal.IndexManagerDao;

/**
 * The DataAccessLayerImpl uses JPA to interact with the underlying database.
 */
public class DataAccessLayerImpl implements DataAccessLayer {
	private EntityManager entityManager;

	@Override
	public IndexManagerDao createIndexManagerDao() {
		return new IndexManagerDaoImpl(entityManager);
	}

	@Override
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}
}
