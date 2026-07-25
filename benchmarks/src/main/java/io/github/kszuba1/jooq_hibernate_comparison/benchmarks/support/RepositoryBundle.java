package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support;

import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerBatchRepository;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderAggregateRepository;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderDetailsRepository;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderListingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ProductSearchRepository;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ReportingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.StockRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateCustomerBatchRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateCustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateOrderAggregateRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateOrderDetailsRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateOrderListingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateProductSearchRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateReportingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateStockRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqCustomerBatchRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqCustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqOrderAggregateRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqOrderDetailsRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqOrderListingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqProductSearchRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqReportingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqStockRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jooq.DSLContext;

public record RepositoryBundle(
		CustomerRepository customers,
		OrderAggregateRepository orderAggregates,
		CustomerBatchRepository customerBatches,
		OrderListingRepository orderListings,
		OrderDetailsRepository orderDetails,
		ReportingRepository reporting,
		ProductSearchRepository productSearch,
		StockRepository stock) {

	public static RepositoryBundle hibernate(EntityManagerFactory emf) {
		return new RepositoryBundle(
				new HibernateCustomerRepository(emf),
				new HibernateOrderAggregateRepository(emf),
				new HibernateCustomerBatchRepository(emf),
				new HibernateOrderListingRepository(emf),
				new HibernateOrderDetailsRepository(emf),
				new HibernateReportingRepository(emf),
				new HibernateProductSearchRepository(emf),
				new HibernateStockRepository(emf));
	}

	public static RepositoryBundle jooq(DSLContext dsl) {
		return jooq(dsl, false);
	}

	public static RepositoryBundle jooq(DSLContext dsl, boolean tunedWrites) {
		return new RepositoryBundle(
				new JooqCustomerRepository(dsl),
				new JooqOrderAggregateRepository(dsl, tunedWrites),
				new JooqCustomerBatchRepository(dsl),
				new JooqOrderListingRepository(dsl),
				new JooqOrderDetailsRepository(dsl),
				new JooqReportingRepository(dsl),
				new JooqProductSearchRepository(dsl),
				new JooqStockRepository(dsl));
	}

}
