package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

public final class JooqContextFactory {

	private JooqContextFactory() {
	}

	public static DSLContext createContext(DataSource dataSource) {
		return DSL.using(dataSource, SQLDialect.POSTGRES);
	}

	public static DSLContext createContext(DataSource dataSource, Settings settings) {
		return DSL.using(dataSource, SQLDialect.POSTGRES, settings);
	}

}
