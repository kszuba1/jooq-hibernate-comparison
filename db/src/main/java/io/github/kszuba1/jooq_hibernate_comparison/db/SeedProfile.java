package io.github.kszuba1.jooq_hibernate_comparison.db;

public enum SeedProfile {

	SMALL(50, 20, 100, 250),
	MEDIUM(2_000, 500, 5_000, 25_000),
	LARGE(10_000, 2_000, 50_000, 250_000);

	private final int customers;
	private final int products;
	private final int orders;
	private final int orderLines;

	SeedProfile(int customers, int products, int orders, int orderLines) {
		this.customers = customers;
		this.products = products;
		this.orders = orders;
		this.orderLines = orderLines;
	}

	public int customers() {
		return customers;
	}

	public int products() {
		return products;
	}

	public int orders() {
		return orders;
	}

	public int orderLines() {
		return orderLines;
	}

}
