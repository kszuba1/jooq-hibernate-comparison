package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "product")
public class ProductEntity {

	@Id
	private UUID id;

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Column(nullable = false)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private BigDecimal price;

	@Column(name = "stock_qty", nullable = false)
	private int stockQty;

	@Version
	@Column(nullable = false)
	private int version;

	protected ProductEntity() {
	}

	UUID getId() {
		return id;
	}

	UUID getCategoryId() {
		return categoryId;
	}

	String getSku() {
		return sku;
	}

	String getName() {
		return name;
	}

	BigDecimal getPrice() {
		return price;
	}

	int getStockQty() {
		return stockQty;
	}

	void setStockQty(int stockQty) {
		this.stockQty = stockQty;
	}

}
