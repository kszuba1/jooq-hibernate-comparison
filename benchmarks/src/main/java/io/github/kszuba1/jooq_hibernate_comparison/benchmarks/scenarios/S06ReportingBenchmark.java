package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.CategoryTreeNode;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductRevenue;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ReportingRepository;
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
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 8, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(value = 2, jvmArgs = { "-Xms2g", "-Xmx2g" })
public class S06ReportingBenchmark {

	@State(Scope.Benchmark)
	public static class ReportingStyle {

		@Param({ "native", "hql", "criteria" })
		public String reportingStyle;

		public ReportingRepository repository;

		@Setup(Level.Trial)
		public void resolve(BenchmarkState state) {
			repository = state.environment.reporting(state.stack, reportingStyle);
		}

	}

	@Benchmark
	public List<ProductRevenue> topProductsPerCategory(ReportingStyle style) {
		return style.repository.topProductsPerCategory(5);
	}

	@Benchmark
	public List<CategoryTreeNode> categoryTree(BenchmarkState state, ReportingStyle style) {
		return style.repository.categoryTree(state.ids.rootCategoryId());
	}

}
