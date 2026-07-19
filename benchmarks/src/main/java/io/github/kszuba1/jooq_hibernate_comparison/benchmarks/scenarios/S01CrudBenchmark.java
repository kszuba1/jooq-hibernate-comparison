package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class S01CrudBenchmark {

	@Benchmark
	public Optional<Customer> findById(BenchmarkState state) {
		return state.repos.customers().findById(state.ids.nextCustomerId());
	}

	@Benchmark
	public Optional<Customer> findByEmail(BenchmarkState state) {
		return state.repos.customers().findByEmail(state.ids.nextSeededEmail());
	}

	@Benchmark
	public void create(BenchmarkState state) {
		state.repos.customers().create(state.newCustomer());
	}

	@Benchmark
	public boolean updateExisting(BenchmarkState state) {
		UUID id = state.ids.nextCustomerId();
		Customer changed = new Customer(id, "upd-" + state.nextUnique() + "@example.com",
				"Updated Customer", null);
		return state.repos.customers().update(changed);
	}

	@Benchmark
	public boolean createThenDelete(BenchmarkState state) {
		Customer customer = state.newCustomer();
		state.repos.customers().create(customer);
		return state.repos.customers().deleteById(customer.id());
	}

}
