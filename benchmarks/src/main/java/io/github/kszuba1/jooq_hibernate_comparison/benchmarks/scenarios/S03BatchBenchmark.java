package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 3, time = 10)
@Fork(2)
public class S03BatchBenchmark {

	@State(Scope.Benchmark)
	public static class BatchInput {

		@Param({ "1000" })
		public int batchSize;
	}

	@Benchmark
	public void insertBatch(BenchmarkState state, BatchInput input) {
		state.repos.customerBatches().insertAll(state.newCustomers(input.batchSize));
	}

	@Benchmark
	public void updateBatch(BenchmarkState state, BatchInput input) {
		int count = Math.min(input.batchSize, state.ids.customerCount());
		List<Customer> updates = state.newCustomers(count).stream()
				.map(c -> new Customer(state.ids.nextCustomerId(), c.email(), c.fullName(), c.createdAt()))
				.toList();
		state.repos.customerBatches().updateAll(updates);
	}

}
