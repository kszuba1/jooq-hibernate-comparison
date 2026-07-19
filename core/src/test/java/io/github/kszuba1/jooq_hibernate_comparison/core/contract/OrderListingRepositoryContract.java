package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderSummary;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class OrderListingRepositoryContract {

	private static final OffsetDateTime BASE = OffsetDateTime.of(2026, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);

	private final UUID customerId = UUID.randomUUID();
	private final List<UUID> newOrdersOldestFirst = new ArrayList<>();

	protected abstract OrderListingRepository repository();

	protected abstract DataSource dataSource();

	@BeforeEach
	void seedOrders() {
		JdbcFixtures.truncateAll(dataSource());
		newOrdersOldestFirst.clear();
		JdbcFixtures.insertCustomer(dataSource(), customerId, "s4.customer@example.com");
		for (int i = 0; i < 5; i++) {
			UUID id = UUID.randomUUID();
			newOrdersOldestFirst.add(id);
			JdbcFixtures.insertOrder(dataSource(), id, customerId, OrderStatus.NEW, BASE.plusHours(i));
		}
		JdbcFixtures.insertOrder(dataSource(), UUID.randomUUID(), customerId, OrderStatus.PAID, BASE.plusDays(1));
		JdbcFixtures.insertOrder(dataSource(), UUID.randomUUID(), customerId, OrderStatus.CANCELLED, BASE.plusDays(2));
	}

	@Test
	void returnsOnlyOrdersWithTheRequestedStatus() {
		List<OrderSummary> result = repository().findByStatus(OrderStatus.NEW, 100, 0);

		assertThat(result).hasSize(5);
		assertThat(result).allSatisfy(summary -> assertThat(summary.status()).isEqualTo(OrderStatus.NEW));
	}

	@Test
	void ordersNewestFirst() {
		List<OrderSummary> result = repository().findByStatus(OrderStatus.NEW, 100, 0);

		List<UUID> expected = new ArrayList<>(newOrdersOldestFirst);
		java.util.Collections.reverse(expected);
		assertThat(result).extracting(OrderSummary::id).containsExactlyElementsOf(expected);
	}

	@Test
	void paginatesWithLimitAndOffset() {
		List<OrderSummary> firstPage = repository().findByStatus(OrderStatus.NEW, 2, 0);
		List<OrderSummary> secondPage = repository().findByStatus(OrderStatus.NEW, 2, 2);
		List<OrderSummary> lastPage = repository().findByStatus(OrderStatus.NEW, 2, 4);

		assertThat(firstPage).extracting(OrderSummary::id)
				.containsExactly(newOrdersOldestFirst.get(4), newOrdersOldestFirst.get(3));
		assertThat(secondPage).extracting(OrderSummary::id)
				.containsExactly(newOrdersOldestFirst.get(2), newOrdersOldestFirst.get(1));
		assertThat(lastPage).extracting(OrderSummary::id).containsExactly(newOrdersOldestFirst.get(0));
	}

	@Test
	void offsetBeyondTheEndReturnsEmpty() {
		assertThat(repository().findByStatus(OrderStatus.NEW, 10, 50)).isEmpty();
	}

	@Test
	void equalTimestampsTieBreakByIdDescending() {
		JdbcFixtures.truncateAll(dataSource());
		JdbcFixtures.insertCustomer(dataSource(), customerId, "s4.ties@example.com");
		OffsetDateTime sameInstant = BASE.plusDays(10).truncatedTo(ChronoUnit.MICROS);
		List<UUID> ids = new ArrayList<>();
		for (int i = 0; i < 8; i++) {
			UUID id = UUID.randomUUID();
			ids.add(id);
			JdbcFixtures.insertOrder(dataSource(), id, customerId, OrderStatus.SHIPPED, sameInstant);
		}

		List<OrderSummary> result = repository().findByStatus(OrderStatus.SHIPPED, 10, 0);

		List<UUID> expected = ids.stream().sorted(JdbcFixtures.PG_UUID_ORDER.reversed()).toList();
		assertThat(result).extracting(OrderSummary::id).containsExactlyElementsOf(expected);
	}

	@Test
	void mapsAllSummaryFields() {
		List<OrderSummary> result = repository().findByStatus(OrderStatus.PAID, 10, 0);

		assertThat(result).hasSize(1);
		OrderSummary summary = result.getFirst();
		assertThat(summary.customerId()).isEqualTo(customerId);
		assertThat(summary.status()).isEqualTo(OrderStatus.PAID);
		assertThat(summary.placedAt().toInstant()).isEqualTo(BASE.plusDays(1).toInstant());
	}

}
