package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.kszuba1.jooq_hibernate_comparison.db.DeterministicSeeder;
import io.github.kszuba1.jooq_hibernate_comparison.db.SeedProfile;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernatePersistenceFactory;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqContextFactory;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class ManualEnvironment implements HarnessEnvironment {

	private final PostgreSQLContainer container;
	private final HikariDataSource dataSource;
	private final EntityManagerFactory entityManagerFactory;

	private ManualEnvironment(PostgreSQLContainer container, HikariDataSource dataSource,
			EntityManagerFactory entityManagerFactory) {
		this.container = container;
		this.dataSource = dataSource;
		this.entityManagerFactory = entityManagerFactory;
	}

	public static ManualEnvironment start(String tier) {
		PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse(BenchmarkImage.get()));
		container.start();
		Flyway.configure()
				.dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
				.load()
				.migrate();

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(container.getJdbcUrl());
		config.setUsername(container.getUsername());
		config.setPassword(container.getPassword());
		config.setMaximumPoolSize(16);
		config.setMinimumIdle(16);
		HikariDataSource dataSource = new HikariDataSource(config);

		new DeterministicSeeder(dataSource, SeedProfile.valueOf(tier), DeterministicSeeder.DEFAULT_SEED).seed();
		EntityManagerFactory emf = HibernatePersistenceFactory.createEntityManagerFactory(dataSource);
		return new ManualEnvironment(container, dataSource, emf);
	}

	@Override
	public RepositoryBundle repositories(String stack) {
		return switch (stack) {
			case "hibernate" -> RepositoryBundle.hibernate(entityManagerFactory);
			case "jooq" -> RepositoryBundle.jooq(JooqContextFactory.createContext(dataSource));
			default -> throw new IllegalArgumentException("unknown stack: " + stack);
		};
	}

	@Override
	public DataSource dataSource() {
		return dataSource;
	}

	@Override
	public void close() {
		entityManagerFactory.close();
		dataSource.close();
		container.stop();
	}

}
