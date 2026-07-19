package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {

	@Id
	private UUID id;

	@Column(name = "customer_id", nullable = false)
	private UUID customerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "placed_at", nullable = false)
	private OffsetDateTime placedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<OrderLineEntity> lines = new ArrayList<>();

	protected OrderEntity() {
	}

	OrderEntity(UUID id, UUID customerId, OrderStatus status, OffsetDateTime placedAt) {
		this.id = id;
		this.customerId = customerId;
		this.status = status;
		this.placedAt = placedAt;
	}

	void addLine(OrderLineEntity line) {
		lines.add(line);
		line.setOrder(this);
	}

	UUID getId() {
		return id;
	}

	UUID getCustomerId() {
		return customerId;
	}

	OrderStatus getStatus() {
		return status;
	}

	OffsetDateTime getPlacedAt() {
		return placedAt;
	}

	List<OrderLineEntity> getLines() {
		return lines;
	}

}
