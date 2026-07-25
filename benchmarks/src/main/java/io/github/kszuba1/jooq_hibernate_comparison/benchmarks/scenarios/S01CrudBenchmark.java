package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
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
public class S01CrudBenchmark {

	@State(Scope.Benchmark)
	public static class CreatedRowsTrim {

		private DataSource dataSource;

		@Setup(Level.Trial)
		public void capture(BenchmarkState state) {
			dataSource = state.environment.dataSource();
		}

		@TearDown(Level.Iteration)
		public void trim() throws SQLException {
			try (Connection connection = dataSource.getConnection();
					Statement statement = connection.createStatement()) {
				statement.execute("delete from customer where email like 'bench-%'");
			}
		}
	}

	@Benchmark
	public Optional<Customer> findById(BenchmarkState state) {
		return state.repos.customers().findById(state.ids.nextCustomerId());
	}

	@Benchmark
	public Optional<Customer> findByEmail(BenchmarkState state) {
		return state.repos.customers().findByEmail(state.ids.nextSeededEmail());
	}

	@Benchmark
	public void create(BenchmarkState state, CreatedRowsTrim trim) {
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
