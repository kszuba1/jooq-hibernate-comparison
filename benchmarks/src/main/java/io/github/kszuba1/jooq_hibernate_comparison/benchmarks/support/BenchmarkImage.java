package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

final class BenchmarkImage {

	private BenchmarkImage() {
	}

	static String get() {
		Properties properties = new Properties();
		try (InputStream in = BenchmarkImage.class.getResourceAsStream("/container.properties")) {
			properties.load(in);
		} catch (IOException e) {
			throw new UncheckedIOException("container.properties missing from classpath", e);
		}
		return properties.getProperty("postgres.image");
	}

}
