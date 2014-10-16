package org.backmeup.index.dal;

import javax.persistence.EntityManager;

/**
 * The DataAccessLayer provides access to any kind of database. It uses Data
 * Access Objects (e.g. IndexManagerDao) to store, retrieve and delete data of a
 * certain database.
 */
public interface DataAccessLayer {

	IndexManagerDao createIndexManagerDao();

	void setEntityManager(EntityManager connection);

}
