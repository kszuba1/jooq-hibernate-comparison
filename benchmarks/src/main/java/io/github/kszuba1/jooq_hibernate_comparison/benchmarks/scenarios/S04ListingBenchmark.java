package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderSummary;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 8, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(value = 2, jvmArgs = { "-Xms2g", "-Xmx2g" })
public class S04ListingBenchmark {

	@Benchmark
	public List<OrderSummary> firstPage(BenchmarkState state) {
		return state.repos.orderListings().findByStatus(OrderStatus.NEW, 20, 0);
	}

	@Benchmark
	public List<OrderSummary> deepPage(BenchmarkState state) {
		return state.repos.orderListings().findByStatus(OrderStatus.NEW, 20, 500);
	}

}
