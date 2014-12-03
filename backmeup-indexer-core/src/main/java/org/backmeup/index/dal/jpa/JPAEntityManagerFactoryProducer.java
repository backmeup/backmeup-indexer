package org.backmeup.index.dal.jpa;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAEntityManagerFactoryProducer {

    private static final String PERSISTENCE_UNIT = "org.backmeup.index.jpa";

    @Produces
    @ApplicationScoped
    public EntityManagerFactory create() {
        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }

    public void destroy(@Disposes EntityManagerFactory factory) {
        if (factory.isOpen())
            factory.close();
    }

}
