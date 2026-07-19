package io.github.kszuba1.jooq_hibernate_comparison.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

public final class DeterministicSeeder {

	public static final long DEFAULT_SEED = 20260712L;

	private static final OffsetDateTime BASE_TIME = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
	private static final int BATCH_SIZE = 500;
	private static final String[] ORDER_STATUSES = { "NEW", "PAID", "SHIPPED", "CANCELLED" };
	private static final String[] TAG_NAMES = { "sale", "premium", "eco", "clearance", "new-arrival", "bestseller",
			"limited", "imported", "handmade", "refurbished" };
	private static final String[] ADJECTIVES = { "Compact", "Deluxe", "Eco", "Smart", "Classic", "Premium", "Basic",
			"Portable", "Heavy-Duty", "Wireless" };
	private static final String[] NOUNS = { "Widget", "Gadget", "Lamp", "Desk", "Chair", "Speaker", "Monitor",
			"Keyboard", "Cable", "Stand" };

	private final DataSource dataSource;
	private final Random random;
	private final SeedProfile profile;

	public DeterministicSeeder(DataSource dataSource, SeedProfile profile, long seed) {
		this.dataSource = dataSource;
		this.profile = profile;
		this.random = new Random(seed);
	}

	public void seed() {
		try (Connection connection = dataSource.getConnection()) {
			boolean autoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			try {
				List<UUID> categories = seedCategories(connection);
				List<UUID> customers = seedCustomers(connection);
				List<UUID> products = seedProducts(connection, categories);
				seedTags(connection, products);
				seedOrdersWithLines(connection, customers, products);
				seedReviews(connection, customers, products);
				connection.commit();
			} catch (SQLException | RuntimeException e) {
				connection.rollback();
				throw e instanceof SQLException sqlException
						? new IllegalStateException("seeding failed", sqlException)
						: (RuntimeException) e;
			} finally {
				connection.setAutoCommit(autoCommit);
			}
			analyze();
		} catch (SQLException e) {
			throw new IllegalStateException("seeding failed", e);
		}
	}

	private List<UUID> seedCategories(Connection connection) throws SQLException {
		List<UUID> all = new ArrayList<>();
		try (PreparedStatement statement = connection
				.prepareStatement("insert into category (id, parent_id, name) values (?, ?, ?)")) {
			int counter = 0;
			for (int rootIndex = 0; rootIndex < 5; rootIndex++) {
				UUID rootId = nextUuid();
				statement.setObject(1, rootId);
				statement.setObject(2, null);
				statement.setString(3, "category-" + counter++);
				statement.addBatch();
				all.add(rootId);
				int children = random.nextInt(5);
				for (int childIndex = 0; childIndex < children; childIndex++) {
					UUID childId = nextUuid();
					statement.setObject(1, childId);
					statement.setObject(2, rootId);
					statement.setString(3, "category-" + counter++);
					statement.addBatch();
					all.add(childId);
				}
			}
			statement.executeBatch();
		}
		return all;
	}

	private List<UUID> seedCustomers(Connection connection) throws SQLException {
		List<UUID> customers = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"insert into customer (id, email, full_name, created_at) values (?, ?, ?, ?)")) {
			for (int i = 0; i < profile.customers(); i++) {
				UUID id = nextUuid();
				customers.add(id);
				statement.setObject(1, id);
				statement.setString(2, "customer" + i + "@seed.example.com");
				statement.setString(3, pick(ADJECTIVES) + " Customer " + i);
				statement.setObject(4, BASE_TIME.plusMinutes(random.nextInt(500_000)));
				statement.addBatch();
				flushEvery(statement, i);
			}
			statement.executeBatch();
		}
		return customers;
	}

	private List<UUID> seedProducts(Connection connection, List<UUID> categories) throws SQLException {
		List<UUID> products = new ArrayList<>();
		try (PreparedStatement statement = connection.prepareStatement(
				"insert into product (id, category_id, sku, name, price, stock_qty, version) values (?, ?, ?, ?, ?, ?, 0)")) {
			for (int i = 0; i < profile.products(); i++) {
				UUID id = nextUuid();
				products.add(id);
				statement.setObject(1, id);
				statement.setObject(2, categories.get(skewedIndex(categories.size())));
				statement.setString(3, "SKU-" + i);
				statement.setString(4, pick(ADJECTIVES) + " " + pick(NOUNS) + " " + i);
				statement.setBigDecimal(5, price(1, 2000));
				statement.setInt(6, random.nextInt(1000) + 100);
				statement.addBatch();
				flushEvery(statement, i);
			}
			statement.executeBatch();
		}
		return products;
	}

	private void seedTags(Connection connection, List<UUID> products) throws SQLException {
		List<UUID> tagIds = new ArrayList<>();
		try (PreparedStatement statement = connection
				.prepareStatement("insert into tag (id, name) values (?, ?)")) {
			for (String tagName : TAG_NAMES) {
				UUID id = nextUuid();
				tagIds.add(id);
				statement.setObject(1, id);
				statement.setString(2, tagName);
				statement.addBatch();
			}
			statement.executeBatch();
		}
		try (PreparedStatement statement = connection
				.prepareStatement("insert into product_tag (product_id, tag_id) values (?, ?)")) {
			int batched = 0;
			for (UUID product : products) {
				int tagCount = random.nextInt(4);
				Set<Integer> chosen = new HashSet<>();
				for (int t = 0; t < tagCount; t++) {
					chosen.add(random.nextInt(tagIds.size()));
				}
				for (int index : chosen.stream().sorted().toList()) {
					statement.setObject(1, product);
					statement.setObject(2, tagIds.get(index));
					statement.addBatch();
					flushEvery(statement, batched++);
				}
			}
			statement.executeBatch();
		}
	}

	private void seedOrdersWithLines(Connection connection, List<UUID> customers, List<UUID> products)
			throws SQLException {
		int averageLines = Math.max(1, profile.orderLines() / profile.orders());
		try (PreparedStatement orderStatement = connection.prepareStatement(
				"insert into orders (id, customer_id, status, placed_at) values (?, ?, ?, ?)");
				PreparedStatement lineStatement = connection.prepareStatement(
						"insert into order_line (id, order_id, product_id, qty, unit_price) values (?, ?, ?, ?, ?)")) {
			for (int i = 0; i < profile.orders(); i++) {
				UUID orderId = nextUuid();
				orderStatement.setObject(1, orderId);
				orderStatement.setObject(2, customers.get(skewedIndex(customers.size())));
				orderStatement.setString(3, ORDER_STATUSES[random.nextInt(ORDER_STATUSES.length)]);
				orderStatement.setObject(4, BASE_TIME.plusMinutes(random.nextInt(500_000)));
				orderStatement.addBatch();

				int lines = 1 + random.nextInt(averageLines * 2);
				Set<UUID> usedProducts = new HashSet<>();
				for (int l = 0; l < lines; l++) {
					UUID productId = products.get(skewedIndex(products.size()));
					if (!usedProducts.add(productId)) {
						continue;
					}
					lineStatement.setObject(1, nextUuid());
					lineStatement.setObject(2, orderId);
					lineStatement.setObject(3, productId);
					lineStatement.setInt(4, 1 + random.nextInt(10));
					lineStatement.setBigDecimal(5, price(1, 2000));
					lineStatement.addBatch();
				}
				if ((i + 1) % BATCH_SIZE == 0) {
					orderStatement.executeBatch();
					lineStatement.executeBatch();
				}
			}
			orderStatement.executeBatch();
			lineStatement.executeBatch();
		}
	}

	private void seedReviews(Connection connection, List<UUID> customers, List<UUID> products)
			throws SQLException {
		int reviews = Math.min(profile.customers() * profile.products(), profile.orders() / 2);
		try (PreparedStatement statement = connection.prepareStatement(
				"insert into review (id, product_id, customer_id, rating, body, created_at) values (?, ?, ?, ?, ?, ?) "
						+ "on conflict (product_id, customer_id) do nothing")) {
			for (int i = 0; i < reviews; i++) {
				statement.setObject(1, nextUuid());
				statement.setObject(2, products.get(skewedIndex(products.size())));
				statement.setObject(3, customers.get(skewedIndex(customers.size())));
				statement.setInt(4, 1 + random.nextInt(5));
				statement.setString(5, random.nextInt(3) == 0 ? null : "Review text " + i);
				statement.setObject(6, BASE_TIME.plusMinutes(random.nextInt(500_000)));
				statement.addBatch();
				flushEvery(statement, i);
			}
			statement.executeBatch();
		}
	}

	private void analyze() throws SQLException {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement()) {
			statement.execute("analyze");
		}
	}

	private UUID nextUuid() {
		return new UUID(random.nextLong(), random.nextLong());
	}

	private int skewedIndex(int size) {
		double draw = random.nextDouble();
		return (int) Math.floor(draw * draw * size);
	}

	private BigDecimal price(int min, int max) {
		long cents = min * 100L + random.nextInt((max - min) * 100 + 1);
		return BigDecimal.valueOf(cents, 2);
	}

	private String pick(String[] values) {
		return values[random.nextInt(values.length)];
	}

	private static void flushEvery(PreparedStatement statement, int counter) throws SQLException {
		if (counter > 0 && counter % BATCH_SIZE == 0) {
			statement.executeBatch();
		}
	}

}
