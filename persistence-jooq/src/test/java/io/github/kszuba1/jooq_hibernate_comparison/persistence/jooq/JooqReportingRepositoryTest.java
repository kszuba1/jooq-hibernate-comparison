package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.ReportingRepositoryContract;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ReportingRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class JooqReportingRepositoryTest extends ReportingRepositoryContract {

	private static TestDatabase database;
	private static ReportingRepository repository;

	@BeforeAll
	static void startDatabase() {
		database = TestDatabase.start();
		repository = new JooqReportingRepository(JooqContextFactory.createContext(database.dataSource()));
	}

	@AfterAll
	static void stopDatabase() {
		if (database != null) {
			database.close();
		}
	}

	@Override
	protected ReportingRepository repository() {
		return repository;
	}

	@Override
	protected DataSource dataSource() {
		return database.dataSource();
	}

}
