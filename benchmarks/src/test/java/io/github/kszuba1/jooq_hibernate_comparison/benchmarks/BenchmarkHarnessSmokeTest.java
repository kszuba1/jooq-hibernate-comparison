package io.github.kszuba1.jooq_hibernate_comparison.benchmarks;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import static org.assertj.core.api.Assertions.assertThat;

class BenchmarkHarnessSmokeTest {

	@Test
	void harnessRunsOneBenchmarkForBothStacks() throws Exception {
		Options options = new OptionsBuilder()
				.include("S01CrudBenchmark.findById$")
				.param("stack", "hibernate", "jooq")
				.param("config", "default")
				.param("tier", "SMALL")
				.forks(0)
				.warmupIterations(1)
				.warmupTime(TimeValue.milliseconds(200))
				.measurementIterations(1)
				.measurementTime(TimeValue.milliseconds(300))
				.build();

		Collection<RunResult> results = new Runner(options).run();

		assertThat(results).hasSize(2);
		assertThat(results).allSatisfy(result ->
				assertThat(result.getPrimaryResult().getStatistics().getN()).isPositive());
	}

	@Test
	void manualWiringEnvironmentRunsTheValidationBenchmark() throws Exception {
		Options options = new OptionsBuilder()
				.include("V01WiringValidationBenchmark.findById$")
				.param("wiring", "manual")
				.param("stack", "hibernate", "jooq")
				.param("tier", "SMALL")
				.forks(0)
				.warmupIterations(1)
				.warmupTime(TimeValue.milliseconds(200))
				.measurementIterations(1)
				.measurementTime(TimeValue.milliseconds(300))
				.build();

		Collection<RunResult> results = new Runner(options).run();

		assertThat(results).hasSize(2);
		assertThat(results).allSatisfy(result ->
				assertThat(result.getPrimaryResult().getStatistics().getN()).isPositive());
	}

}
