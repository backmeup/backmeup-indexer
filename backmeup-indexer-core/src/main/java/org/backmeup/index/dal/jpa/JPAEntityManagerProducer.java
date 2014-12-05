package org.backmeup.index.dal.jpa;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class JPAEntityManagerProducer {

    @Resource
    private EntityManagerFactory entityManagerFactory;

    @Produces
    @RequestScoped
    public EntityManager create() {
        return this.entityManagerFactory.createEntityManager();
    }

    public void destroy(@Disposes EntityManager manager) {
        if (manager.isOpen())
            manager.close();
    }

}
