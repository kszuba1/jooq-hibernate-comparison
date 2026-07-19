package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.OrderDetailsRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderDetailsRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class HibernateOrderDetailsRepositoryTest extends OrderDetailsRepositoryContract {

	private static TestDatabase database;
	private static EntityManagerFactory entityManagerFactory;
	private static OrderDetailsRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		entityManagerFactory = HibernatePersistenceFactory.createEntityManagerFactory(database.dataSource());
		repository = new HibernateOrderDetailsRepository(entityManagerFactory);
	}

	@AfterAll
	static void stopDatabase() {
		if (entityManagerFactory != null) {
			entityManagerFactory.close();
		}
		if (database != null) {
			database.close();
		}
	}

	@Override
	protected OrderDetailsRepository repository() {
		return repository;
	}

	@Override
	protected DataSource dataSource() {
		return database.dataSource();
	}

}
