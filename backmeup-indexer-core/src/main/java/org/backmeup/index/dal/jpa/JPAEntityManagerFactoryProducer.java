package org.backmeup.index.dal.jpa;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

@Alternative
public class JPAEntityManagerFactoryProducer {

	@Alternative
	@Produces
	@ApplicationScoped
	public EntityManagerFactory create() {
		return Persistence.createEntityManagerFactory("org.backmeup.index.jpa");
	}

	public void destroy(@Disposes EntityManagerFactory factory) {
		if (factory.isOpen())
			factory.close();
	}
}
