package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.ProductSearchRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ProductSearchRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class JooqProductSearchRepositoryTest extends ProductSearchRepositoryContract {

	private static TestDatabase database;
	private static ProductSearchRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		repository = new JooqProductSearchRepository(JooqContextFactory.createContext(database.dataSource()));
	}

	@AfterAll
	static void stopDatabase() {
		if (database != null) {
			database.close();
		}
	}

	@Override
	protected ProductSearchRepository repository() {
		return repository;
	}

	@Override
	protected DataSource dataSource() {
		return database.dataSource();
	}

}
