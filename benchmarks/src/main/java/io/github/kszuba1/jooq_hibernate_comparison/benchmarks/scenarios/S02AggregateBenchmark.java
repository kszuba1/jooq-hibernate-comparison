package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.scenarios;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support.BenchmarkState;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Order;
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
public class S02AggregateBenchmark {

	@State(Scope.Benchmark)
	public static class CreatedOrdersTrim {

		private DataSource dataSource;

		@Setup(Level.Trial)
		public void capture(BenchmarkState state) {
			dataSource = state.environment.dataSource();
		}

		@TearDown(Level.Iteration)
		public void trim() throws SQLException {
			try (Connection connection = dataSource.getConnection();
					Statement statement = connection.createStatement()) {
				statement.execute("delete from order_line where order_id in "
						+ "(select id from orders where placed_at >= timestamptz '2026-01-01')");
				statement.execute("delete from orders where placed_at >= timestamptz '2026-01-01'");
			}
		}
	}

	@Benchmark
	public void createAggregate(BenchmarkState state, CreatedOrdersTrim trim) {
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
