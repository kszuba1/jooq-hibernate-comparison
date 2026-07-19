package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.OrderAggregateRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderAggregateRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class HibernateOrderAggregateRepositoryTest extends OrderAggregateRepositoryContract {

	private static TestDatabase database;
	private static EntityManagerFactory entityManagerFactory;
	private static OrderAggregateRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		entityManagerFactory = HibernatePersistenceFactory.createEntityManagerFactory(database.dataSource());
		repository = new HibernateOrderAggregateRepository(entityManagerFactory);
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
	protected OrderAggregateRepository repository() {
		return repository;
	}

	@Override
	protected DataSource dataSource() {
		return database.dataSource();
	}

}
