package io.github.kszuba1.jooq_hibernate_comparison.benchmarks;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.PostgresImage;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateCustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernatePersistenceFactory;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqContextFactory;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqCustomerRepository;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class S01CreateRoundTripDiagnostic {

	private static final int WARMUP = 5;
	private static final int TRACED = 3;

	@Test
	void printWireTraceForBothStacks() {
		try (PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse(PostgresImage.get()))) {
			container.withCommand("postgres", "-c", "fsync=off", "-c", "log_statement=all");
			container.start();
			Flyway.configure()
					.dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
					.load()
					.migrate();

			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(container.getJdbcUrl());
			config.setUsername(container.getUsername());
			config.setPassword(container.getPassword());
			config.setMaximumPoolSize(1);
			config.setMinimumIdle(1);

			try (HikariDataSource dataSource = new HikariDataSource(config)) {
				EntityManagerFactory emf = HibernatePersistenceFactory.createEntityManagerFactory(dataSource);
				CustomerRepository hibernate = new HibernateCustomerRepository(emf);
				CustomerRepository jooq = new JooqCustomerRepository(JooqContextFactory.createContext(dataSource));

				for (int i = 0; i < WARMUP; i++) {
					hibernate.create(customer());
					jooq.create(customer());
				}

				int before = logLineCount(container);
				for (int i = 0; i < TRACED; i++) {
					hibernate.create(customer());
				}
				List<String> hibernateTrace = tailStatements(container, before);

				before = logLineCount(container);
				for (int i = 0; i < TRACED; i++) {
					jooq.create(customer());
				}
				List<String> jooqTrace = tailStatements(container, before);

				print("HIBERNATE", hibernateTrace);
				print("JOOQ", jooqTrace);
				emf.close();
			}
		}
	}

	private static Customer customer() {
		UUID id = UUID.randomUUID();
		return new Customer(id, "diag-" + id + "@example.com", "Diagnostic",
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	private static int logLineCount(PostgreSQLContainer container) {
		return container.getLogs().split("\n", -1).length;
	}

	private static List<String> tailStatements(PostgreSQLContainer container, int fromLine) {
		String[] lines = container.getLogs().split("\n", -1);
		return Arrays.stream(lines, Math.min(fromLine, lines.length), lines.length)
				.filter(l -> l.contains("statement:") || l.contains("execute "))
				.map(l -> l.replaceAll(".*(statement:|execute [^:]*:)\\s*", "").trim())
				.toList();
	}

	private static void print(String label, List<String> statements) {
		System.out.println("=== " + label + ": " + statements.size() + " wire operations for " + TRACED
				+ " create() calls (" + String.format("%.1f", statements.size() / (double) TRACED) + " per call)");
		statements.forEach(s -> System.out.println("      " + (s.length() > 100 ? s.substring(0, 100) + "..." : s)));
	}

}
