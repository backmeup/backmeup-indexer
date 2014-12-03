package org.backmeup.index.dal.jpa;

import java.lang.reflect.ParameterizedType;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.backmeup.index.dal.BaseDao;

/**
 * Realizes the CRUD operations for a model class <T> based on the JPA
 * (EntityManager).
 * 
 * @param <T>
 *            The model class to use
 */
public abstract class BaseDaoImpl<T> implements BaseDao<T> {
	protected EntityManager em;
	private Class<T> entityClass;

	@SuppressWarnings("unchecked")
	public BaseDaoImpl(EntityManager em) {
		this.em = em;
		ParameterizedType superType = (ParameterizedType) this.getClass()
				.getGenericSuperclass();
		entityClass = (Class<T>) superType.getActualTypeArguments()[0];
	}

	@Override
	public T merge(T entity) {
		return em.merge(entity);
	}

	@Override
	public T findById(long id) {
		T item = em.find(entityClass, id);
		return item;
	}

	@Override
	public boolean delete(T entity) {
		entity = em.merge(entity);
		em.remove(entity);
		return true;
	}

	@Override
	public T save(T entity) {
		entity = em.merge(entity);
		return entity;
	}

	@Override
	public long count() {
		Query q = em.createQuery("SELECT COUNT(u) FROM "
				+ entityClass.getName() + " u");
		Long cnt = (Long) q.getSingleResult();
		return cnt;
	}
}
