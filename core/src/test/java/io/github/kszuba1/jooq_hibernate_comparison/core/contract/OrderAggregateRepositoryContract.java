package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Order;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderLine;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderAggregateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class OrderAggregateRepositoryContract {

	private final UUID customerId = UUID.randomUUID();
	private final UUID categoryId = UUID.randomUUID();
	private final UUID productA = UUID.randomUUID();
	private final UUID productB = UUID.randomUUID();
	private final UUID productC = UUID.randomUUID();

	protected abstract OrderAggregateRepository repository();

	protected abstract DataSource dataSource();

	@BeforeEach
	void seedPrerequisites() {
		JdbcFixtures.truncateAll(dataSource());
		JdbcFixtures.insertCustomer(dataSource(), customerId, "s2.customer@example.com");
		JdbcFixtures.insertCategory(dataSource(), categoryId, null, "s2-category");
		JdbcFixtures.insertProduct(dataSource(), productA, categoryId, "S2-A", "Product A", new BigDecimal("10.00"), 100);
		JdbcFixtures.insertProduct(dataSource(), productB, categoryId, "S2-B", "Product B", new BigDecimal("20.00"), 100);
		JdbcFixtures.insertProduct(dataSource(), productC, categoryId, "S2-C", "Product C", new BigDecimal("30.00"), 100);
	}

	@Test
	void createThenFindByIdRoundTripsTheAggregate() {
		Order order = newOrder(
				new OrderLine(UUID.randomUUID(), productA, 2, new BigDecimal("10.00")),
				new OrderLine(UUID.randomUUID(), productB, 1, new BigDecimal("20.00")));
		repository().create(order);

		Optional<Order> found = repository().findById(order.id());

		assertThat(found).isPresent();
		Order actual = found.orElseThrow();
		assertThat(actual.id()).isEqualTo(order.id());
		assertThat(actual.customerId()).isEqualTo(customerId);
		assertThat(actual.status()).isEqualTo(OrderStatus.NEW);
		assertThat(actual.placedAt().toInstant()).isEqualTo(order.placedAt().toInstant());
		assertSameLines(actual.lines(), order.lines());
	}

	@Test
	void createIsVisibleToPlainJdbc() {
		Order order = newOrder(
				new OrderLine(UUID.randomUUID(), productA, 2, new BigDecimal("10.00")),
				new OrderLine(UUID.randomUUID(), productB, 1, new BigDecimal("20.00")));
		repository().create(order);

		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from orders where id = ?", order.id()))
				.isEqualTo(1);
		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from order_line where order_id = ?",
				order.id())).isEqualTo(2);
	}

	@Test
	void createWithUnknownProductRollsBackTheWholeAggregate() {
		Order order = newOrder(
				new OrderLine(UUID.randomUUID(), productA, 1, new BigDecimal("10.00")),
				new OrderLine(UUID.randomUUID(), UUID.randomUUID(), 1, new BigDecimal("99.99")));

		assertThatThrownBy(() -> repository().create(order)).isInstanceOf(RuntimeException.class);

		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from orders")).isZero();
		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from order_line")).isZero();
	}

	@Test
	void createSupportsAnOrderWithoutLines() {
		Order order = newOrder();
		repository().create(order);

		Optional<Order> found = repository().findById(order.id());

		assertThat(found).isPresent();
		assertThat(found.orElseThrow().lines()).isEmpty();
	}

	@Test
	void findByIdReturnsEmptyForUnknownOrder() {
		assertThat(repository().findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void findByIdReturnsLinesSortedByProductId() {
		Order order = newOrder(
				new OrderLine(UUID.randomUUID(), productC, 3, new BigDecimal("30.00")),
				new OrderLine(UUID.randomUUID(), productA, 1, new BigDecimal("10.00")),
				new OrderLine(UUID.randomUUID(), productB, 2, new BigDecimal("20.00")));
		repository().create(order);

		List<OrderLine> lines = repository().findById(order.id()).orElseThrow().lines();

		assertThat(lines).extracting(OrderLine::productId)
				.containsExactlyElementsOf(
						List.of(productA, productB, productC).stream().sorted(JdbcFixtures.PG_UUID_ORDER).toList());
	}

	@Test
	void deleteRemovesTheOrderAndItsLinesOnly() {
		Order order = newOrder(
				new OrderLine(UUID.randomUUID(), productA, 2, new BigDecimal("10.00")),
				new OrderLine(UUID.randomUUID(), productB, 1, new BigDecimal("20.00")));
		repository().create(order);

		boolean deleted = repository().deleteById(order.id());

		assertThat(deleted).isTrue();
		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from orders")).isZero();
		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from order_line")).isZero();
		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from product")).isEqualTo(3);
		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from customer")).isEqualTo(1);
	}

	@Test
	void deleteReturnsFalseForUnknownOrder() {
		assertThat(repository().deleteById(UUID.randomUUID())).isFalse();
	}

	private Order newOrder(OrderLine... lines) {
		return new Order(UUID.randomUUID(), customerId, OrderStatus.NEW,
				OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS), List.of(lines));
	}

	private static void assertSameLines(List<OrderLine> actual, List<OrderLine> expected) {
		List<OrderLine> expectedSorted = expected.stream()
				.sorted(Comparator.comparing(OrderLine::productId, JdbcFixtures.PG_UUID_ORDER)).toList();
		assertThat(actual).hasSameSizeAs(expectedSorted);
		for (int i = 0; i < actual.size(); i++) {
			OrderLine a = actual.get(i);
			OrderLine e = expectedSorted.get(i);
			assertThat(a.id()).isEqualTo(e.id());
			assertThat(a.productId()).isEqualTo(e.productId());
			assertThat(a.quantity()).isEqualTo(e.quantity());
			assertThat(a.unitPrice()).isEqualByComparingTo(e.unitPrice());
		}
	}

}
