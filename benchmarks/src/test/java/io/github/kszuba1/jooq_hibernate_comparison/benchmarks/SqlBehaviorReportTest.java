package io.github.kszuba1.jooq_hibernate_comparison.benchmarks;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.contract.TestDatabase;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Order;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderLine;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductFilter;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.db.DeterministicSeeder;
import io.github.kszuba1.jooq_hibernate_comparison.db.SeedProfile;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateCustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateOrderAggregateRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateOrderDetailsRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateOrderListingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernatePersistenceFactory;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateProductSearchRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateReportingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate.HibernateStockRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqContextFactory;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqCustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqOrderAggregateRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqOrderDetailsRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqOrderListingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqProductSearchRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqReportingRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.JooqStockRepository;
import jakarta.persistence.EntityManagerFactory;
import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlBehaviorReportTest {

	private record Row(String scenario, String operation, String stack, long select, long insert, long update,
			long delete, long total) {
	}

	private static TestDatabase database;
	private static DataSource proxied;
	private static EntityManagerFactory emf;
	private static final List<Row> rows = new ArrayList<>();

	private static UUID customerId;
	private static UUID orderId;
	private static UUID productId;
	private static UUID rootCategoryId;

	@BeforeAll
	static void setUp() throws SQLException {
		database = TestDatabase.start();
		new DeterministicSeeder(database.dataSource(), SeedProfile.SMALL, DeterministicSeeder.DEFAULT_SEED)
				.seed();
		proxied = ProxyDataSourceBuilder.create(database.dataSource()).name("sql-report").countQuery().build();
		emf = HibernatePersistenceFactory.createEntityManagerFactory(proxied);
		try (Connection connection = database.dataSource().getConnection();
				Statement statement = connection.createStatement()) {
			customerId = queryId(statement, "select id from customer order by id limit 1");
			productId = queryId(statement, "select id from product order by id limit 1");
			rootCategoryId = queryId(statement, "select id from category where parent_id is null order by name limit 1");
			orderId = queryId(statement,
					"select order_id from order_line group by order_id order by count(*) desc, order_id limit 1");
		}
	}

	@AfterAll
	static void tearDown() throws IOException {
		Path report = Path.of("target", "sql-behavior.md");
		StringBuilder md = new StringBuilder("# SQL behavior per scenario operation\n\n")
				.append("Statement counts captured by datasource-proxy, seed profile SMALL, one execution each.\n\n")
				.append("| Scenario | Operation | Stack | SELECT | INSERT | UPDATE | DELETE | Total |\n")
				.append("|---|---|---|---|---|---|---|---|\n");
		for (Row row : rows) {
			md.append("| ").append(row.scenario()).append(" | ").append(row.operation()).append(" | ")
					.append(row.stack()).append(" | ").append(row.select()).append(" | ").append(row.insert())
					.append(" | ").append(row.update()).append(" | ").append(row.delete()).append(" | ")
					.append(row.total()).append(" |\n");
		}
		Files.createDirectories(report.getParent());
		Files.writeString(report, md.toString());
		if (emf != null) {
			emf.close();
		}
		if (database != null) {
			database.close();
		}
	}

	@Test
	void captureStatementCountsForBothStacks() {
		Map<String, CustomerRepository> s1 = new LinkedHashMap<>();
		s1.put("hibernate", new HibernateCustomerRepository(emf));
		s1.put("jooq", new JooqCustomerRepository(JooqContextFactory.createContext(proxied)));

		var jooqDsl = JooqContextFactory.createContext(proxied);

		for (String stack : List.of("hibernate", "jooq")) {
			boolean hibernate = stack.equals("hibernate");

			CustomerRepository customers = s1.get(stack);
			capture("S1", "findById", stack, () -> customers.findById(customerId));
			capture("S1", "create", stack, () -> customers.create(freshCustomer()));
			Customer toUpdate = freshCustomer();
			customers.create(toUpdate);
			capture("S1", "update", stack, () -> customers.update(new Customer(toUpdate.id(),
					"upd." + toUpdate.email(), "Updated", toUpdate.createdAt())));
			capture("S1", "delete", stack, () -> customers.deleteById(toUpdate.id()));

			var aggregates = hibernate ? new HibernateOrderAggregateRepository(emf)
					: new JooqOrderAggregateRepository(jooqDsl);
			Order created = freshOrder();
			capture("S2", "create 3-line aggregate", stack, () -> aggregates.create(created));
			capture("S2", "findById", stack, () -> aggregates.findById(orderId));
			capture("S2", "delete aggregate", stack, () -> aggregates.deleteById(created.id()));

			var listings = hibernate ? new HibernateOrderListingRepository(emf)
					: new JooqOrderListingRepository(jooqDsl);
			capture("S4", "page 20/0", stack, () -> listings.findByStatus(OrderStatus.NEW, 20, 0));

			var details = hibernate ? new HibernateOrderDetailsRepository(emf)
					: new JooqOrderDetailsRepository(jooqDsl);
			capture("S5", "detailsForCustomer", stack, () -> details.findDetailsByCustomer(customerId));

			var reporting = hibernate ? new HibernateReportingRepository(emf)
					: new JooqReportingRepository(jooqDsl);
			capture("S6", "top5 per category", stack, () -> reporting.topProductsPerCategory(5));
			capture("S6", "categoryTree", stack, () -> reporting.categoryTree(rootCategoryId));

			var search = hibernate ? new HibernateProductSearchRepository(emf)
					: new JooqProductSearchRepository(jooqDsl);
			capture("S7", "full filter", stack, () -> search.search(new ProductFilter("a", null,
					new BigDecimal("1.00"), new BigDecimal("2000.00"), true, "sale")));

			var stock = hibernate ? new HibernateStockRepository(emf) : new JooqStockRepository(jooqDsl);
			capture("S8", "uncontended decrement", stack, () -> stock.decrementStock(productId, 1));
		}

		assertThat(rows).isNotEmpty();
		assertThat(rows).filteredOn(r -> r.scenario().equals("S5"))
				.allSatisfy(r -> assertThat(r.total()).isEqualTo(1));
		assertThat(rows).filteredOn(r -> r.scenario().equals("S4"))
				.allSatisfy(r -> assertThat(r.total()).isEqualTo(1));
		assertThat(rows).filteredOn(r -> r.scenario().equals("S2") && r.operation().equals("findById"))
				.allSatisfy(r -> assertThat(r.total()).isEqualTo(2));
	}

	private static void capture(String scenario, String operation, String stack, Runnable work) {
		QueryCountHolder.clear();
		work.run();
		QueryCount count = QueryCountHolder.getGrandTotal();
		rows.add(new Row(scenario, operation, stack, count.getSelect(), count.getInsert(), count.getUpdate(),
				count.getDelete(),
				count.getSelect() + count.getInsert() + count.getUpdate() + count.getDelete() + count.getOther()));
	}

	private static Customer freshCustomer() {
		UUID id = UUID.randomUUID();
		return new Customer(id, "report-" + id + "@example.com", "Report Customer",
				OffsetDateTime.now(ZoneOffset.UTC));
	}

	private static Order freshOrder() throws IllegalStateException {
		try (Connection connection = database.dataSource().getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select id from product order by id limit 3")) {
			List<OrderLine> lines = new ArrayList<>();
			int qty = 1;
			while (rs.next()) {
				lines.add(new OrderLine(UUID.randomUUID(), rs.getObject(1, UUID.class), qty++,
						new BigDecimal("9.99")));
			}
			return new Order(UUID.randomUUID(), customerId, OrderStatus.NEW,
					OffsetDateTime.now(ZoneOffset.UTC), lines);
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
	}

	private static UUID queryId(Statement statement, String sql) throws SQLException {
		try (ResultSet rs = statement.executeQuery(sql)) {
			rs.next();
			return rs.getObject(1, UUID.class);
		}
	}

}
