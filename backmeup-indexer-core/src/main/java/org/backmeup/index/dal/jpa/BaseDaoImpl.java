package org.backmeup.index.dal.jpa;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.backmeup.index.dal.BaseDao;

/**
 * Realizes the CRUD operations for a model class <T> based on the JPA (EntityManager).
 * 
 * @param <T>
 *            The model class to use
 */
public abstract class BaseDaoImpl<T> implements BaseDao<T> {
    @Inject
    protected EntityManager entityManager;

    private final Class<T> entityClass;

    public BaseDaoImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Transactional
    @Override
    public T merge(T entity) {
        return this.entityManager.merge(entity);
    }

    @Override
    public T findById(long id) {
        T item = this.entityManager.find(this.entityClass, id);
        return item;
    }

    @Transactional
    @Override
    public boolean delete(T entity) {
        T deletedEntity = this.entityManager.merge(entity);
        this.entityManager.remove(deletedEntity);
        return true;
    }

    @Transactional
    @Override
    public T save(T entity) {
        T savedEntity = this.entityManager.merge(entity);
        return savedEntity;
    }

    @Override
    public long count() {
        Query q = this.entityManager.createQuery("SELECT COUNT(u) FROM " + this.entityClass.getName() + " u");
        return (Long) q.getSingleResult();
    }
}
