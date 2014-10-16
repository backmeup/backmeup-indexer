package org.backmeup.index.dal.jpa;

import javax.persistence.EntityManager;

import org.backmeup.index.dal.DataAccessLayer;
import org.backmeup.index.dal.IndexManagerDao;

/**
 * The DataAccessLayerImpl uses JPA to interact with the underlying database.
 * 
 */
// context and dependency injection cdi in JavaEE
@javax.enterprise.context.ApplicationScoped
public class DataAccessLayerImpl implements DataAccessLayer {
	private final ThreadLocal<EntityManager> threaLocalEntityManager = new ThreadLocal<>();

	public DataAccessLayerImpl() {
	}

	@Override
	public IndexManagerDao createIndexManagerDao() {
		return new IndexManagerDaoImpl(threaLocalEntityManager.get());
	}

	@Override
	public void setConnection(Object connection) {
		this.threaLocalEntityManager.set((EntityManager) connection);
	}
}
