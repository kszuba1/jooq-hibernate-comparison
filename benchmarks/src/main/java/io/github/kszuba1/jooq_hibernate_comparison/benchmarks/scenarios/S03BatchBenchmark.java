package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
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
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 3, time = 10)
@Fork(value = 2, jvmArgs = { "-Xms2g", "-Xmx2g" })
public class S03BatchBenchmark {

	@State(Scope.Thread)
	public static class BatchInput {

		@Param({ "1000" })
		public int batchSize;

		List<Customer> inserts;
		List<Customer> updates;

		@Setup(Level.Invocation)
		public void prepare(BenchmarkState state) throws SQLException {
			try (Connection connection = state.environment.dataSource().getConnection();
					Statement statement = connection.createStatement()) {
				statement.execute("delete from customer where email like 'bench-%'");
			}
			inserts = state.newCustomers(batchSize);
			int updateCount = Math.min(batchSize, state.ids.customerCount());
			updates = new ArrayList<>(updateCount);
			OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
			for (int i = 0; i < updateCount; i++) {
				UUID unique = BenchmarkState.cheapUuid();
				updates.add(new Customer(state.ids.nextCustomerId(), "upd-" + unique + "@example.com",
						"Updated Customer", now));
			}
		}
	}

	@Benchmark
	public void insertBatch(BenchmarkState state, BatchInput input) {
		state.repos.customerBatches().insertAll(input.inserts);
	}

	@Benchmark
	public void updateBatch(BenchmarkState state, BatchInput input) {
		state.repos.customerBatches().updateAll(input.updates);
	}

}
