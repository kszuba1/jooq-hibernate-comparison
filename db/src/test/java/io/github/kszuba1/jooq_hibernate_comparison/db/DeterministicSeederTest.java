package io.github.kszuba1.jooq_hibernate_comparison.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicSeederTest {

	private static final List<String> TABLES = List.of("customer", "category", "product", "orders", "order_line",
			"review", "tag", "product_tag");

	@Test
	void sameSeedProducesIdenticalDataInTwoSeparateDatabases() {
		Map<String, String> first;
		Map<String, String> second;
		try (TestDatabase databaseA = TestDatabase.start(); TestDatabase databaseB = TestDatabase.start()) {
			new DeterministicSeeder(databaseA.dataSource(), SeedProfile.SMALL, DeterministicSeeder.DEFAULT_SEED)
					.seed();
			new DeterministicSeeder(databaseB.dataSource(), SeedProfile.SMALL, DeterministicSeeder.DEFAULT_SEED)
					.seed();
			first = fingerprint(databaseA.dataSource());
			second = fingerprint(databaseB.dataSource());
		}

		assertThat(first).isEqualTo(second);
		assertThat(first.get("order_line")).doesNotStartWith("0:");
	}

	@Test
	void differentSeedProducesDifferentData() {
		Map<String, String> first;
		Map<String, String> second;
		try (TestDatabase databaseA = TestDatabase.start(); TestDatabase databaseB = TestDatabase.start()) {
			new DeterministicSeeder(databaseA.dataSource(), SeedProfile.SMALL, 1L).seed();
			new DeterministicSeeder(databaseB.dataSource(), SeedProfile.SMALL, 2L).seed();
			first = fingerprint(databaseA.dataSource());
			second = fingerprint(databaseB.dataSource());
		}

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void mediumProfileCrossesBatchBoundariesWithoutFkViolations() {
		try (TestDatabase database = TestDatabase.start()) {
			new DeterministicSeeder(database.dataSource(), SeedProfile.MEDIUM, DeterministicSeeder.DEFAULT_SEED)
					.seed();
			Map<String, String> fingerprint = fingerprint(database.dataSource());

			assertThat(rowCount(fingerprint, "orders")).isEqualTo(SeedProfile.MEDIUM.orders());
			assertThat(rowCount(fingerprint, "customer")).isEqualTo(SeedProfile.MEDIUM.customers());
			assertThat(rowCount(fingerprint, "order_line")).isGreaterThan(SeedProfile.MEDIUM.orders());
		}
	}

	@Test
	void seededVolumesMatchTheProfileScale() {
		try (TestDatabase database = TestDatabase.start()) {
			new DeterministicSeeder(database.dataSource(), SeedProfile.SMALL, DeterministicSeeder.DEFAULT_SEED)
					.seed();
			Map<String, String> fingerprint = fingerprint(database.dataSource());

			assertThat(rowCount(fingerprint, "customer")).isEqualTo(SeedProfile.SMALL.customers());
			assertThat(rowCount(fingerprint, "product")).isEqualTo(SeedProfile.SMALL.products());
			assertThat(rowCount(fingerprint, "orders")).isEqualTo(SeedProfile.SMALL.orders());
			assertThat(rowCount(fingerprint, "order_line")).isPositive();
		}
	}

	private static Map<String, String> fingerprint(DataSource dataSource) {
		Map<String, String> result = new LinkedHashMap<>();
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			for (String table : TABLES) {
				try (ResultSet rs = statement.executeQuery(
						"select count(*), coalesce(sum(hashtext(t::text)::bigint), 0) from " + table + " t")) {
					rs.next();
					result.put(table, rs.getLong(1) + ":" + rs.getLong(2));
				}
			}
		} catch (SQLException e) {
			throw new IllegalStateException("fingerprint failed", e);
		}
		return result;
	}

	private static int rowCount(Map<String, String> fingerprint, String table) {
		return Integer.parseInt(fingerprint.get(table).split(":")[0]);
	}

}
