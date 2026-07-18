package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.CustomerRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class JooqCustomerRepositoryTest extends CustomerRepositoryContract {

	private static TestDatabase database;
	private static CustomerRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		repository = new JooqCustomerRepository(JooqContextFactory.createContext(database.dataSource()));
	}

	@AfterAll
	static void stopDatabase() {
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
