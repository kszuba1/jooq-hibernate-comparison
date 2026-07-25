package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkEnvironment;
import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.HarnessEnvironment;
import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.IdPools;
import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.ManualEnvironment;
import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.RepositoryBundle;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderSummary;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 8, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(value = 2, jvmArgs = { "-Xms2g", "-Xmx2g" })
public class V01WiringValidationBenchmark {

	@State(Scope.Benchmark)
	public static class WiringState {

		@Param({ "spring", "manual" })
		public String wiring;

		@Param({ "hibernate", "jooq" })
		public String stack;

		@Param({ "SMALL" })
		public String tier;

		HarnessEnvironment environment;
		RepositoryBundle repos;
		IdPools ids;

		@Setup(Level.Trial)
		public void setUp() {
			environment = switch (wiring) {
				case "spring" -> BenchmarkEnvironment.start(tier, "default");
				case "manual" -> ManualEnvironment.start(tier);
				default -> throw new IllegalArgumentException("unknown wiring: " + wiring);
			};
			repos = environment.repositories(stack);
			ids = IdPools.load(environment.dataSource());
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			environment.close();
		}
	}

	@Benchmark
	public Optional<Customer> findById(WiringState state) {
		return state.repos.customers().findById(state.ids.nextCustomerId());
	}

	@Benchmark
	public List<OrderSummary> firstPage(WiringState state) {
		return state.repos.orderListings().findByStatus(OrderStatus.NEW, 20, 0);
	}

}
