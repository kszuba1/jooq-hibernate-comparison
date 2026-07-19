package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.OrderListingRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderListingRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class HibernateOrderListingRepositoryTest extends OrderListingRepositoryContract {

	private static TestDatabase database;
	private static EntityManagerFactory entityManagerFactory;
	private static OrderListingRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		entityManagerFactory = HibernatePersistenceFactory.createEntityManagerFactory(database.dataSource());
		repository = new HibernateOrderListingRepository(entityManagerFactory);
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
	protected OrderListingRepository repository() {
		return repository;
	}

	@Override
	protected DataSource dataSource() {
		return database.dataSource();
	}

}
