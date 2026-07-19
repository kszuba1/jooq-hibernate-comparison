package io.github.kszuba1.jooq_hibernate_comparison.core.repository;

import java.util.Optional;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Order;

public interface OrderAggregateRepository {

	void create(Order order);

	Optional<Order> findById(UUID id);

	boolean deleteById(UUID id);

}
