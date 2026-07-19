package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderDetails;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderLineDetails;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderDetailsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class OrderDetailsRepositoryContract {

	private static final OffsetDateTime BASE = OffsetDateTime.of(2026, 2, 1, 9, 0, 0, 0, ZoneOffset.UTC);

	private final UUID customer = UUID.randomUUID();
	private final UUID otherCustomer = UUID.randomUUID();
	private final UUID categoryId = UUID.randomUUID();
	private final UUID productA = UUID.randomUUID();
	private final UUID productB = UUID.randomUUID();
	private final UUID productC = UUID.randomUUID();
	private final UUID order1 = UUID.randomUUID();
	private final UUID order2 = UUID.randomUUID();
	private final UUID emptyOrder = UUID.randomUUID();

	protected abstract OrderDetailsRepository repository();

	protected abstract DataSource dataSource();

	@BeforeEach
	void seedOrders() {
		DataSource ds = dataSource();
		JdbcFixtures.truncateAll(ds);
		JdbcFixtures.insertCustomer(ds, customer, "s5.customer@example.com");
		JdbcFixtures.insertCustomer(ds, otherCustomer, "s5.other@example.com");
		JdbcFixtures.insertCategory(ds, categoryId, null, "s5-category");
		JdbcFixtures.insertProduct(ds, productA, categoryId, "S5-A", "Alpha Widget", new BigDecimal("10.00"), 10);
		JdbcFixtures.insertProduct(ds, productB, categoryId, "S5-B", "Beta Widget", new BigDecimal("20.00"), 10);
		JdbcFixtures.insertProduct(ds, productC, categoryId, "S5-C", "Gamma Widget", new BigDecimal("30.00"), 10);

		JdbcFixtures.insertOrder(ds, order1, customer, OrderStatus.NEW, BASE);
		JdbcFixtures.insertOrderLine(ds, UUID.randomUUID(), order1, productA, 2, new BigDecimal("10.00"));
		JdbcFixtures.insertOrderLine(ds, UUID.randomUUID(), order1, productB, 1, new BigDecimal("20.00"));

		JdbcFixtures.insertOrder(ds, order2, customer, OrderStatus.PAID, BASE.plusHours(1));
		JdbcFixtures.insertOrderLine(ds, UUID.randomUUID(), order2, productC, 5, new BigDecimal("30.00"));

		JdbcFixtures.insertOrder(ds, emptyOrder, customer, OrderStatus.CANCELLED, BASE.plusHours(2));

		UUID foreignOrder = UUID.randomUUID();
		JdbcFixtures.insertOrder(ds, foreignOrder, otherCustomer, OrderStatus.NEW, BASE.plusHours(3));
		JdbcFixtures.insertOrderLine(ds, UUID.randomUUID(), foreignOrder, productA, 9, new BigDecimal("10.00"));
	}

	@Test
	void returnsAllOrdersOfTheCustomerNewestFirst() {
		List<OrderDetails> result = repository().findDetailsByCustomer(customer);

		assertThat(result).extracting(OrderDetails::orderId).containsExactly(emptyOrder, order2, order1);
	}

	@Test
	void resolvesProductNamesOnLines() {
		List<OrderDetails> result = repository().findDetailsByCustomer(customer);

		OrderDetails first = result.stream().filter(o -> o.orderId().equals(order1)).findFirst().orElseThrow();
		List<UUID> expectedProductOrder = List.of(productA, productB).stream()
				.sorted(JdbcFixtures.PG_UUID_ORDER).toList();
		assertThat(first.lines()).extracting(OrderLineDetails::productId)
				.containsExactlyElementsOf(expectedProductOrder);
		assertThat(first.lines()).extracting(OrderLineDetails::productName)
				.containsExactlyInAnyOrder("Alpha Widget", "Beta Widget");
	}

	@Test
	void includesOrdersWithoutLines() {
		List<OrderDetails> result = repository().findDetailsByCustomer(customer);

		OrderDetails empty = result.stream().filter(o -> o.orderId().equals(emptyOrder)).findFirst().orElseThrow();
		assertThat(empty.lines()).isEmpty();
		assertThat(empty.status()).isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void excludesOrdersOfOtherCustomers() {
		List<OrderDetails> result = repository().findDetailsByCustomer(customer);

		assertThat(result).hasSize(3);
	}

	@Test
	void returnsEmptyForUnknownCustomer() {
		assertThat(repository().findDetailsByCustomer(UUID.randomUUID())).isEmpty();
	}

	@Test
	void equalTimestampsTieBreakByIdDescending() {
		DataSource ds = dataSource();
		OffsetDateTime sameInstant = BASE.plusDays(5);
		List<UUID> ids = new java.util.ArrayList<>();
		for (int i = 0; i < 8; i++) {
			UUID id = UUID.randomUUID();
			ids.add(id);
			JdbcFixtures.insertOrder(ds, id, otherCustomer, OrderStatus.NEW, sameInstant);
		}

		List<OrderDetails> result = repository().findDetailsByCustomer(otherCustomer);

		List<UUID> expected = ids.stream().sorted(JdbcFixtures.PG_UUID_ORDER.reversed()).toList();
		assertThat(result.stream().filter(o -> ids.contains(o.orderId())).map(OrderDetails::orderId).toList())
				.containsExactlyElementsOf(expected);
	}

	@Test
	void mapsLineFieldsCompletely() {
		List<OrderDetails> result = repository().findDetailsByCustomer(customer);

		OrderDetails second = result.stream().filter(o -> o.orderId().equals(order2)).findFirst().orElseThrow();
		assertThat(second.lines()).hasSize(1);
		OrderLineDetails line = second.lines().getFirst();
		assertThat(line.productId()).isEqualTo(productC);
		assertThat(line.productName()).isEqualTo("Gamma Widget");
		assertThat(line.quantity()).isEqualTo(5);
		assertThat(line.unitPrice()).isEqualByComparingTo(new BigDecimal("30.00"));
	}

}
