package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;

public final class HibernatePersistenceFactory {

	private HibernatePersistenceFactory() {
	}

	public static EntityManagerFactory createEntityManagerFactory(DataSource dataSource) {
		return createEntityManagerFactory(dataSource, Map.of());
	}

	public static EntityManagerFactory createEntityManagerFactory(DataSource dataSource,
			Map<String, Object> extraProperties) {
		PersistenceConfiguration configuration = new PersistenceConfiguration("jooq-hibernate-comparison")
				.property("jakarta.persistence.nonJtaDataSource", dataSource)
				.managedClass(CategoryEntity.class)
				.managedClass(CustomerEntity.class)
				.managedClass(OrderEntity.class)
				.managedClass(OrderLineEntity.class)
				.managedClass(ProductEntity.class)
				.managedClass(TagEntity.class)
				.managedClass(ProductTagEntity.class)
				.property("hibernate.hbm2ddl.auto", "validate");
		extraProperties.forEach(configuration::property);
		return configuration.createEntityManagerFactory();
	}

}
