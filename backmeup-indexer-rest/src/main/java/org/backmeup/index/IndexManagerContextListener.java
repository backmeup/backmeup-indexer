package org.backmeup.index;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class IndexManagerContextListener implements ServletContextListener {

    private static final String JNDI_NAME = "java:comp/env/BeanManager";

    private BeanManager beanManager;
    private IndexManagerLifeCycle lifeCycle;

    @Override
    public void contextInitialized(@SuppressWarnings("unused") ServletContextEvent event) {
        getLifeCycleFromContainerAsListenersAreNotSupportedByWeld();
        lifeCycle.initialized();
    }

    private void getLifeCycleFromContainerAsListenersAreNotSupportedByWeld() {
        getBeanManagerFromJndi();
        getLifeCycleFromContainer();
    }

    private void getBeanManagerFromJndi() {
        try {
            InitialContext context = new InitialContext();
            beanManager = (BeanManager) context.lookup(JNDI_NAME);
        } catch (NamingException e) {
            throw new IllegalArgumentException(JNDI_NAME, e);
        }
    }

    private void getLifeCycleFromContainer() {
        lifeCycle = getBean(IndexManagerLifeCycle.class);
    }

    @SuppressWarnings("unchecked")
    private <T> T getBean(Class<T> type) {
        Bean<T> bean = (Bean<T>) beanManager.resolve(beanManager.getBeans(type));
        CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);
        return (T) beanManager.getReference(bean, type, creationalContext);
    }

    @Override
    public void contextDestroyed(@SuppressWarnings("unused") ServletContextEvent event) {
        lifeCycle.destroyed();
    }

}
