package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.OrderListingRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderListingRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class JooqOrderListingRepositoryTest extends OrderListingRepositoryContract {

	private static TestDatabase database;
	private static OrderListingRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		repository = new JooqOrderListingRepository(JooqContextFactory.createContext(database.dataSource()));
	}

	@AfterAll
	static void stopDatabase() {
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
