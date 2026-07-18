package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

public final class HibernatePersistenceFactory {

	private HibernatePersistenceFactory() {
	}

	public static EntityManagerFactory createEntityManagerFactory(DataSource dataSource) {
		return new PersistenceConfiguration("jooq-hibernate-comparison")
				.property("jakarta.persistence.nonJtaDataSource", dataSource)
				.managedClass(CustomerEntity.class)
				.property("hibernate.hbm2ddl.auto", "validate")
				.createEntityManagerFactory();
	}

}
