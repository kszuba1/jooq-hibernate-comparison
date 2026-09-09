package io.github.kszuba1.jooq_hibernate_comparison.benchmarks;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.JdbcFixtures;
import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderDetails;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderDetailsRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateNaiveOrderDetailsRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateOrderDetailsRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernatePersistenceFactory;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqContextFactory;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqOrderDetailsRepository;
import jakarta.persistence.EntityManagerFactory;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class S05NPlusOneReportTest {

	private static final UUID CUSTOMER = new UUID(0, 1);
	private static final UUID CATEGORY = new UUID(0, 1);
	private static final OffsetDateTime BASE = OffsetDateTime.parse("2026-02-01T09:00:00Z");
	private record Row(int orders, int products, boolean sharedProducts, String variant, int queries) {
	}

	private static TestDatabase database;
	private static EntityManagerFactory emf;
	private static final List<String> statements = new ArrayList<>();
	private static final Map<String, OrderDetailsRepository> repositories = new LinkedHashMap<>();
	private static final List<Row> rows = new ArrayList<>();
	private static List<String> naiveExample = List.of();

	@BeforeAll
	static void setUp() {
		database = TestDatabase.start();
		DataSource proxied = ProxyDataSourceBuilder.create(database.dataSource())
				.name("s05-n-plus-one")
				.listener(new QueryExecutionListener() {
					@Override
					public void beforeQuery(ExecutionInfo info, List<QueryInfo> queries) {
					}

					@Override
					public void afterQuery(ExecutionInfo info, List<QueryInfo> queries) {
						queries.forEach(query -> statements.add(query.getQuery()));
					}
				}).build();
		emf = HibernatePersistenceFactory.createEntityManagerFactory(proxied);
		repositories.put("hibernate-naive", new HibernateNaiveOrderDetailsRepository(emf));
		repositories.put("hibernate-join-fetch", new HibernateOrderDetailsRepository(emf));
		repositories.put("jooq-multiset", new JooqOrderDetailsRepository(JooqContextFactory.createContext(proxied)));
	}

	@AfterAll
	static void tearDown() {
		if (emf != null) {
			emf.close();
		}
		if (database != null) {
			database.close();
		}
	}

	@ParameterizedTest(name = "orders={0}, products shared between orders={1}")
	@CsvSource({ "0,true", "1,true", "10,true", "100,true", "10,false", "100,false" })
	void lazyLoadingAddsQueriesPerOrderAndDistinctProduct(int orderCount, boolean sharedProducts)
			throws IOException {
		seedHistory(orderCount, sharedProducts);
		int productCount = orderCount == 0 ? 0 : sharedProducts ? 3 : 3 * orderCount;
		List<OrderDetails> expected = null;
		List<Row> verifiedRows = new ArrayList<>();
		for (var entry : repositories.entrySet()) {
			boolean naive = entry.getKey().equals("hibernate-naive");
			for (int repeat = 0; repeat < 2; repeat++) {
				statements.clear();
				List<OrderDetails> result = entry.getValue().findDetailsByCustomer(CUSTOMER);
				assertThat(result).hasSize(orderCount)
						.allSatisfy(order -> assertThat(order.lines()).hasSize(3));
				if (expected == null) {
					expected = result;
				} else {
					assertThat(result).usingRecursiveComparison()
							.withComparatorForType(Comparator.comparing(OffsetDateTime::toInstant),
									OffsetDateTime.class)
							.isEqualTo(expected);
				}
				assertThat(statements).allMatch(sql -> sql.startsWith("select "))
						.hasSize(naive ? 1 + orderCount + productCount : 1);
				if (naive) {
					assertThat(queriesFrom("orders")).isEqualTo(1);
					assertThat(queriesFrom("order_line")).isEqualTo(orderCount);
					assertThat(queriesFrom("product")).isEqualTo(productCount);
					if (orderCount == 10 && sharedProducts) {
						naiveExample = statements.stream().distinct().toList();
					}
				}
				if (repeat == 0) {
					verifiedRows.add(new Row(orderCount, productCount, sharedProducts, entry.getKey(),
							statements.size()));
				}
			}
		}
		rows.addAll(verifiedRows);
		writeReport();
	}

	private static long queriesFrom(String table) {
		return statements.stream().filter(sql -> sql.matches("(?s).*\\bfrom\\s+" + table + "\\b.*")).count();
	}

	private static void seedHistory(int orderCount, boolean sharedProducts) {
		DataSource ds = database.dataSource();
		JdbcFixtures.truncateAll(ds);
		JdbcFixtures.insertCustomer(ds, CUSTOMER, "s05-n-plus-one@example.com");
		JdbcFixtures.insertCategory(ds, CATEGORY, null, "S5 N+1");
		int productCount = orderCount == 0 ? 0 : sharedProducts ? 3 : orderCount * 3;
		for (int p = 0; p < productCount; p++) {
			JdbcFixtures.insertProduct(ds, new UUID(0, p + 1), CATEGORY, "N1-" + p, "Product " + p,
					new BigDecimal("19.99"), 100);
		}
		for (int i = 0; i < orderCount; i++) {
			UUID orderId = new UUID(0, i + 1);
			JdbcFixtures.insertOrder(ds, orderId, CUSTOMER, OrderStatus.PAID, BASE.plusSeconds(i));
			for (int p = 0; p < 3; p++) {
				UUID productId = new UUID(0, (sharedProducts ? p : i * 3 + p) + 1);
				JdbcFixtures.insertOrderLine(ds, new UUID(0, i * 3 + p + 1), orderId, productId, p + 1,
						new BigDecimal("19.99"));
			}
		}
	}

	private static void writeReport() throws IOException {
		StringBuilder md = new StringBuilder("""
				# S5: N+1 in a naive Hibernate implementation

				Three lines per order, the same DTOs from every variant. Fixture setup is not counted.

				| Orders | Products | Shared | Variant | SELECTs |
				|---:|---:|---|---|---:|
				""");
		rows.stream()
				.sorted(Comparator.comparingInt(Row::orders).thenComparingInt(Row::products)
						.thenComparing(Row::variant))
				.forEach(row -> md.append("| %d | %d | %s | %s | %d |\n".formatted(row.orders(),
						row.products(), row.sharedProducts(), row.variant(), row.queries())));
		if (!naiveExample.isEmpty()) {
			md.append("\n## Naive SQL, 10 orders with 3 shared products\n\n```sql\n");
			naiveExample.forEach(sql -> md.append(sql).append(";\n"));
			md.append("```\n");
		}
		Path path = Path.of("target", "s05-n-plus-one.md");
		Files.createDirectories(path.getParent());
		Files.writeString(path, md);
	}

}
