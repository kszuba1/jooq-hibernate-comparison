package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.CustomerRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class HibernateCustomerRepositoryTest extends CustomerRepositoryContract {

	private static TestDatabase database;
	private static EntityManagerFactory entityManagerFactory;
	private static CustomerRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		entityManagerFactory = HibernatePersistenceFactory.createEntityManagerFactory(database.dataSource());
		repository = new HibernateCustomerRepository(entityManagerFactory);
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
	protected CustomerRepository repository() {
		return repository;
	}

	@Override
	protected DataSource dataSource() {
		return database.dataSource();
	}

}
