package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_line")
public class OrderLineEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private OrderEntity order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private ProductEntity product;

	@Column(nullable = false)
	private int qty;

	@Column(name = "unit_price", nullable = false)
	private BigDecimal unitPrice;

	protected OrderLineEntity() {
	}

	OrderLineEntity(UUID id, ProductEntity product, int qty, BigDecimal unitPrice) {
		this.id = id;
		this.product = product;
		this.qty = qty;
		this.unitPrice = unitPrice;
	}

	void setOrder(OrderEntity order) {
		this.order = order;
	}

	UUID getId() {
		return id;
	}

	ProductEntity getProduct() {
		return product;
	}

	int getQty() {
		return qty;
	}

	BigDecimal getUnitPrice() {
		return unitPrice;
	}

}
