package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support;

import javax.sql.DataSource;

public interface HarnessEnvironment extends AutoCloseable {

	RepositoryBundle repositories(String stack);

	DataSource dataSource();

	@Override
	void close();

}
