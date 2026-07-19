package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support;

import java.util.Map;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.BenchmarksApplication;
import io.github.kszuba1.jooq_hibernate_comparison.db.DeterministicSeeder;
import io.github.kszuba1.jooq_hibernate_comparison.db.SeedProfile;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class BenchmarkEnvironment implements HarnessEnvironment {

	private final PostgreSQLContainer container;
	private final ConfigurableApplicationContext context;

	private BenchmarkEnvironment(PostgreSQLContainer container, ConfigurableApplicationContext context) {
		this.container = container;
		this.context = context;
	}

	public static BenchmarkEnvironment start(String tier, String config) {
		PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse(BenchmarkImage.get()));
		container.start();
		Flyway.configure()
				.dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
				.load()
				.migrate();

		SpringApplication application = new SpringApplication(BenchmarksApplication.class);
		application.setDefaultProperties(Map.of(
				"spring.datasource.url", container.getJdbcUrl(),
				"spring.datasource.username", container.getUsername(),
				"spring.datasource.password", container.getPassword(),
				"spring.datasource.hikari.maximum-pool-size", "16",
				"spring.datasource.hikari.minimum-idle", "16",
				"spring.flyway.enabled", "false",
				"spring.main.banner-mode", "off",
				"benchmark.config", config));
		ConfigurableApplicationContext context = application.run();

		new DeterministicSeeder(context.getBean(DataSource.class), SeedProfile.valueOf(tier),
				DeterministicSeeder.DEFAULT_SEED).seed();
		return new BenchmarkEnvironment(container, context);
	}

	@Override
	public RepositoryBundle repositories(String stack) {
		return context.getBean(stack + "Repositories", RepositoryBundle.class);
	}

	@Override
	public DataSource dataSource() {
		return context.getBean(DataSource.class);
	}

	@Override
	public void close() {
		context.close();
		container.stop();
	}

}
