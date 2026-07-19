package io.github.kszuba1.jooq_hibernate_comparison.benchmarks.support;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

public final class IdPools {

	private final UUID[] customerIds;
	private final UUID[] orderIds;
	private final UUID[] productIds;
	private final UUID rootCategoryId;
	private final UUID hotProductId;
	private final int seededCustomerCount;

	private final AtomicInteger customerCursor = new AtomicInteger();
	private final AtomicInteger orderCursor = new AtomicInteger();
	private final AtomicInteger productCursor = new AtomicInteger();
	private final AtomicInteger emailCursor = new AtomicInteger();

	private IdPools(UUID[] customerIds, UUID[] orderIds, UUID[] productIds, UUID rootCategoryId,
			UUID hotProductId) {
		this.customerIds = customerIds;
		this.orderIds = orderIds;
		this.productIds = productIds;
		this.rootCategoryId = rootCategoryId;
		this.hotProductId = hotProductId;
		this.seededCustomerCount = customerIds.length;
	}

	public static IdPools load(DataSource dataSource) {
		try (Connection connection = dataSource.getConnection()) {
			UUID[] customers = queryIds(connection, "select id from customer order by id");
			UUID[] orders = queryIds(connection, "select id from orders order by id");
			UUID[] products = queryIds(connection, "select id from product order by id");
			UUID root = queryIds(connection,
					"select id from category where parent_id is null order by name limit 1")[0];
			UUID hotProduct = UUID.randomUUID();
			try (PreparedStatement statement = connection.prepareStatement(
					"insert into product (id, category_id, sku, name, price, stock_qty, version) "
							+ "values (?, ?, 'BENCH-HOT', 'Hot Row Product', 9.99, 2000000000, 0)")) {
				statement.setObject(1, hotProduct);
				statement.setObject(2, root);
				statement.execute();
			}
			return new IdPools(customers, orders, products, root, hotProduct);
		} catch (SQLException e) {
			throw new IllegalStateException("loading id pools failed", e);
		}
	}

	private static UUID[] queryIds(Connection connection, String sql) throws SQLException {
		List<UUID> ids = new ArrayList<>();
		try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
			while (rs.next()) {
				ids.add(rs.getObject(1, UUID.class));
			}
		}
		return ids.toArray(UUID[]::new);
	}

	public UUID nextCustomerId() {
		return customerIds[Math.floorMod(customerCursor.getAndIncrement(), customerIds.length)];
	}

	public UUID nextOrderId() {
		return orderIds[Math.floorMod(orderCursor.getAndIncrement(), orderIds.length)];
	}

	public UUID nextProductId() {
		return productIds[Math.floorMod(productCursor.getAndIncrement(), productIds.length)];
	}

	public UUID[] nextDistinctProductIds(int n) {
		int base = productCursor.getAndAdd(n);
		UUID[] result = new UUID[n];
		for (int i = 0; i < n; i++) {
			result[i] = productIds[Math.floorMod(base + i, productIds.length)];
		}
		return result;
	}

	public String nextSeededEmail() {
		int index = Math.floorMod(emailCursor.getAndIncrement(), seededCustomerCount);
		return "customer" + index + "@seed.example.com";
	}

	public UUID rootCategoryId() {
		return rootCategoryId;
	}

	public UUID hotProductId() {
		return hotProductId;
	}

	public int customerCount() {
		return customerIds.length;
	}

}
