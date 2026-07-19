package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Order;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderLine;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

@State(Scope.Benchmark)
public class BenchmarkState {

	@Param({ "hibernate", "jooq" })
	public String stack;

	@Param({ "default", "tuned" })
	public String config;

	@Param({ "SMALL" })
	public String tier;

	public BenchmarkEnvironment environment;
	public RepositoryBundle repos;
	public IdPools ids;

	private final AtomicLong uniqueCounter = new AtomicLong();

	@Setup(Level.Trial)
	public void setUp() {
		environment = BenchmarkEnvironment.start(tier, config);
		repos = environment.repositories(stack);
		ids = IdPools.load(environment.dataSource());
	}

	@TearDown(Level.Trial)
	public void tearDown() {
		environment.close();
	}

	public Customer newCustomer() {
		UUID id = UUID.randomUUID();
		return new Customer(id, "bench-" + id + "@example.com", "Bench Customer",
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	public List<Customer> newCustomers(int count) {
		List<Customer> customers = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			customers.add(newCustomer());
		}
		return customers;
	}

	public Order newOrder() {
		UUID orderId = UUID.randomUUID();
		UUID[] products = ids.nextDistinctProductIds(3);
		List<OrderLine> lines = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			lines.add(new OrderLine(UUID.randomUUID(), products[i], 1 + i, new BigDecimal("19.99")));
		}
		return new Order(orderId, ids.nextCustomerId(), OrderStatus.NEW,
				OffsetDateTime.now(ZoneOffset.UTC), lines);
	}

	public long nextUnique() {
		return uniqueCounter.getAndIncrement();
	}

}
