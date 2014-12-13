package org.backmeup.index.dal.jpa;

import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.backmeup.index.dal.BaseDao;
import org.backmeup.index.dal.Transactional;

/**
 * Realizes the CRUD operations for a model class <T> based on the JPA
 * (EntityManager).
 * 
 * @param <T> The model class to use
 */
public abstract class BaseDaoImpl<T> implements BaseDao<T> {
    @Inject
    protected EntityManager entityManager;

    private final Class<T> entityClass;

    public BaseDaoImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T merge(T entity) {
        return entityManager.merge(entity);
    }

    @Override
    public T findById(long id) {
        T item = entityManager.find(entityClass, id);
        return item;
    }

    @Transactional
    @Override
    public boolean delete(T entity) {
        T deletedEntity = entityManager.merge(entity);
        entityManager.remove(deletedEntity);
        return true;
    }

    @Transactional
    @Override
    public T save(T entity) {
        T savedEntity = entityManager.merge(entity);
        return savedEntity;
    }

    @Override
    public long count() {
        Query q = entityManager.createQuery("SELECT COUNT(u) FROM " + entityClass.getName() + " u");
        return (Long) q.getSingleResult();
    }
}
