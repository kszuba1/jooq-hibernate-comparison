package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.OrderDetailsRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderDetailsRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class JooqOrderDetailsRepositoryTest extends OrderDetailsRepositoryContract {

	private static TestDatabase database;
	private static OrderDetailsRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		repository = new JooqOrderDetailsRepository(JooqContextFactory.createContext(database.dataSource()));
	}

	@AfterAll
	static void stopDatabase() {
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
