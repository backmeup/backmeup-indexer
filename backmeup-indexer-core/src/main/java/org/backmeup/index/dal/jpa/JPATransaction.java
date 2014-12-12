package org.backmeup.index.dal.jpa;

import java.util.concurrent.Callable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.backmeup.index.dal.Transaction;

/**
 * Starts and stops transactions.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@ApplicationScoped
public class JPATransaction implements Transaction {

    @Inject
    private EntityManager entityManager;

    @Override
    public <T> T inside(final Callable<T> getter) {
        try {
            begin();

            T response = getter.call();

            commit();
            return response;
        } catch (RuntimeException e) {
            rollback();
            throw e;
        } catch (Exception e) {
            rollback();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void inside(Runnable call) {
        inside(callableFrom(call));
    }

    private void begin() {
        entityManager.getTransaction().begin();
    }

    private void commit() {
        entityManager.getTransaction().commit();
    }

    private void rollback() {
        entityManager.getTransaction().rollback();
    }

    private static Callable<Void> callableFrom(final Runnable runnable) {
        return new Callable<Void>() {
            @Override
            public Void call() {
                runnable.run(); // NOSONAR we are not running Threads but reusing Runnable as function/closure. 
                return null;
            }
        };
    }

}
