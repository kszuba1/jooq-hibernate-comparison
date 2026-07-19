package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import java.util.List;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderSummary;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderListingRepository;
import org.jooq.DSLContext;

import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.ORDERS;

public class JooqOrderListingRepository implements OrderListingRepository {

	private final DSLContext dsl;

	public JooqOrderListingRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public List<OrderSummary> findByStatus(OrderStatus status, int limit, int offset) {
		return dsl.select(ORDERS.ID, ORDERS.CUSTOMER_ID, ORDERS.STATUS, ORDERS.PLACED_AT)
				.from(ORDERS)
				.where(ORDERS.STATUS.eq(status.name()))
				.orderBy(ORDERS.PLACED_AT.desc(), ORDERS.ID.desc())
				.limit(limit)
				.offset(offset)
				.fetch(record -> new OrderSummary(record.value1(), record.value2(),
						OrderStatus.valueOf(record.value3()), record.value4()));
	}

}
