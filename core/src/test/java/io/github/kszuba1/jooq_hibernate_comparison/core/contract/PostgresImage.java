package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public final class PostgresImage {

	private PostgresImage() {
	}

	public static String get() {
		Properties properties = new Properties();
		try (InputStream in = PostgresImage.class.getResourceAsStream("/container.properties")) {
			properties.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("container.properties missing from test classpath", e);
		}
		return properties.getProperty("postgres.image");
	}

}
