package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;

public final class JdbcFixtures {

	public static final java.util.Comparator<UUID> PG_UUID_ORDER = java.util.Comparator
			.comparingLong((UUID u) -> u.getMostSignificantBits() ^ Long.MIN_VALUE)
			.thenComparingLong(u -> u.getLeastSignificantBits() ^ Long.MIN_VALUE);

	private JdbcFixtures() {
	}

	public static void truncateAll(DataSource dataSource) {
		execute(dataSource,
				"truncate table customer, category, product, orders, order_line, review, tag, product_tag cascade");
	}

	public static void insertCustomer(DataSource dataSource, UUID id, String email) {
		execute(dataSource, "insert into customer (id, email, full_name, created_at) values (?, ?, ?, ?)",
				id, email, "Fixture Customer", OffsetDateTime.now(ZoneOffset.UTC));
	}

	public static void insertCategory(DataSource dataSource, UUID id, UUID parentId, String name) {
		execute(dataSource, "insert into category (id, parent_id, name) values (?, ?, ?)", id, parentId, name);
	}

	public static void insertProduct(DataSource dataSource, UUID id, UUID categoryId, String sku, String name,
			BigDecimal price, int stockQty) {
		execute(dataSource,
				"insert into product (id, category_id, sku, name, price, stock_qty, version) values (?, ?, ?, ?, ?, ?, 0)",
				id, categoryId, sku, name, price, stockQty);
	}

	public static void insertOrder(DataSource dataSource, UUID id, UUID customerId, OrderStatus status,
			OffsetDateTime placedAt) {
		execute(dataSource, "insert into orders (id, customer_id, status, placed_at) values (?, ?, ?, ?)",
				id, customerId, status.name(), placedAt);
	}

	public static void insertOrderLine(DataSource dataSource, UUID id, UUID orderId, UUID productId, int qty,
			BigDecimal unitPrice) {
		execute(dataSource,
				"insert into order_line (id, order_id, product_id, qty, unit_price) values (?, ?, ?, ?, ?)",
				id, orderId, productId, qty, unitPrice);
	}

	public static void insertTag(DataSource dataSource, UUID id, String name) {
		execute(dataSource, "insert into tag (id, name) values (?, ?)", id, name);
	}

	public static void insertProductTag(DataSource dataSource, UUID productId, UUID tagId) {
		execute(dataSource, "insert into product_tag (product_id, tag_id) values (?, ?)", productId, tagId);
	}

	public static int queryInt(DataSource dataSource, String sql, Object... params) {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = prepare(connection, sql, params);
				ResultSet rs = statement.executeQuery()) {
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			throw new IllegalStateException("fixture query failed: " + sql, e);
		}
	}

	public static void execute(DataSource dataSource, String sql, Object... params) {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = prepare(connection, sql, params)) {
			statement.execute();
		} catch (SQLException e) {
			throw new IllegalStateException("fixture statement failed: " + sql, e);
		}
	}

	private static PreparedStatement prepare(Connection connection, String sql, Object... params)
			throws SQLException {
		PreparedStatement statement = connection.prepareStatement(sql);
		for (int i = 0; i < params.length; i++) {
			statement.setObject(i + 1, params[i]);
		}
		return statement;
	}

}
