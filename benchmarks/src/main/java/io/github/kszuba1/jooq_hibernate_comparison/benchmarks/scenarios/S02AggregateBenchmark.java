package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Order;
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
public class S02AggregateBenchmark {

	@Benchmark
	public void createAggregate(BenchmarkState state) {
		state.repos.orderAggregates().create(state.newOrder());
	}

	@Benchmark
	public Optional<Order> findAggregateById(BenchmarkState state) {
		return state.repos.orderAggregates().findById(state.ids.nextOrderId());
	}

	@Benchmark
	public boolean createThenDeleteAggregate(BenchmarkState state) {
		Order order = state.newOrder();
		state.repos.orderAggregates().create(order);
		return state.repos.orderAggregates().deleteById(order.id());
	}

}
