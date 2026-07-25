package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support;

import java.util.Map;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernatePersistenceFactory;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqContextFactory;
import jakarta.persistence.EntityManagerFactory;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Lazy
@Configuration(proxyBeanMethods = false)
class PersistenceWiring {

	private static final Map<String, Object> TUNED_HIBERNATE = Map.of(
			"hibernate.jdbc.batch_size", "50",
			"hibernate.order_inserts", "true",
			"hibernate.order_updates", "true",
			"hibernate.criteria.plan_cache_enabled", "true");

	@Bean(destroyMethod = "close")
	EntityManagerFactory entityManagerFactory(DataSource dataSource,
			@Value("${benchmark.config:default}") String config) {
		Map<String, Object> extra = "tuned".equals(config) ? TUNED_HIBERNATE : Map.of();
		return HibernatePersistenceFactory.createEntityManagerFactory(dataSource, extra);
	}

	@Bean
	DSLContext dslContext(DataSource dataSource) {
		return JooqContextFactory.createContext(dataSource);
	}

	@Bean
	RepositoryBundle hibernateRepositories(EntityManagerFactory emf) {
		return RepositoryBundle.hibernate(emf);
	}

	@Bean
	RepositoryBundle jooqRepositories(DSLContext dsl, @Value("${benchmark.config:default}") String config) {
		return RepositoryBundle.jooq(dsl, "tuned".equals(config));
	}

}
