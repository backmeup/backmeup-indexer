package org.backmeup.index.dal.jpa;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In J2SE (as is Tomcat without EE capabilities) we need to handle our
 * transactions manually.
 * 
 * @author <a href="http://www.code-cop.org/">Peter Kofler</a>
 */
@Transactional
@Interceptor
public class JPATransactionInterceptor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private EntityManager entityManager;

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception {
        try {
            log.debug("starting transaction for " + ctx.getMethod().getName());
            begin();

            Object response = ctx.proceed();

            commit();
            log.debug("commited transaction for " + ctx.getMethod().getName());
            return response;
        } catch (Exception e) {
            rollback();
            throw e;
        }
    }

    private void begin() {
        transaction().begin();
    }

    private void commit() {
        transaction().commit();
    }

    private void rollback() {
        transaction().rollback();
    }

    private EntityTransaction transaction() {
        return entityManager.getTransaction();
    }

}
