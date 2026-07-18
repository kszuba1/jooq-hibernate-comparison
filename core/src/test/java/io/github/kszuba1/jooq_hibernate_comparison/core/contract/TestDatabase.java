package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public final class TestDatabase implements AutoCloseable {

	private final PostgreSQLContainer container;
	private final DataSource dataSource;

	private TestDatabase(PostgreSQLContainer container, DataSource dataSource) {
		this.container = container;
		this.dataSource = dataSource;
	}

	public static TestDatabase start() {
		PostgreSQLContainer container = new PostgreSQLContainer(DockerImageName.parse(PostgresImage.get()));
		container.start();
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setURL(container.getJdbcUrl());
		dataSource.setUser(container.getUsername());
		dataSource.setPassword(container.getPassword());
		Flyway.configure().dataSource(dataSource).load().migrate();
		return new TestDatabase(container, dataSource);
	}

	public DataSource dataSource() {
		return dataSource;
	}

	@Override
	public void close() {
		container.stop();
	}

}
