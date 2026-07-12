package io.github.kszuba1.jooq_hibernate_comparison.benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse(postgresImage()));
	}

	static String postgresImage() {
		Properties properties = new Properties();
		try (InputStream in = TestcontainersConfiguration.class.getResourceAsStream("/container.properties")) {
			properties.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("container.properties missing from test classpath", e);
		}
		return properties.getProperty("postgres.image");
	}

}
