package io.github.kszuba1.jooq_hibernate_comparison.benchmarks;

import org.openjdk.jmh.annotations.Benchmark;

public class PipelineSmokeBenchmark {

	@Benchmark
	public long baselineNanoTime() {
		return System.nanoTime();
	}

}
