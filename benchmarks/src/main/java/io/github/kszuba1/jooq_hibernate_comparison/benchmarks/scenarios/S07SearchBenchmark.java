package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductFilter;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductSummary;
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
public class S07SearchBenchmark {

	private static final ProductFilter NAME_ONLY = new ProductFilter("smart", null, null, null, false, null);

	private static final ProductFilter FULL_FILTER = new ProductFilter("e", null, new BigDecimal("10.00"),
			new BigDecimal("1500.00"), true, "sale");

	@Benchmark
	public List<ProductSummary> singlePredicate(BenchmarkState state) {
		return state.repos.productSearch().search(NAME_ONLY);
	}

	@Benchmark
	public List<ProductSummary> fullFilter(BenchmarkState state) {
		return state.repos.productSearch().search(FULL_FILTER);
	}

}
