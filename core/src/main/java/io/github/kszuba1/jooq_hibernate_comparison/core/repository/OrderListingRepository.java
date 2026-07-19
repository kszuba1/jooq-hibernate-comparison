package io.github.kszuba1.jooq_hibernate_comparison.core.repository;

import java.util.List;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderSummary;

public interface OrderListingRepository {

	List<OrderSummary> findByStatus(OrderStatus status, int limit, int offset);

}
