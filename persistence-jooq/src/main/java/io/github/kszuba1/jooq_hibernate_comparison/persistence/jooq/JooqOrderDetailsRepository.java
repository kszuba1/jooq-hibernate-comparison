package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderDetails;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderLineDetails;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderDetailsRepository;
import org.jooq.DSLContext;

import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.ORDERS;
import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.ORDER_LINE;
import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.PRODUCT;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.select;

public class JooqOrderDetailsRepository implements OrderDetailsRepository {

	private final DSLContext dsl;

	public JooqOrderDetailsRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public List<OrderDetails> findDetailsByCustomer(UUID customerId) {
		return dsl.select(
				ORDERS.ID,
				ORDERS.STATUS,
				ORDERS.PLACED_AT,
				multiset(
						select(ORDER_LINE.PRODUCT_ID, PRODUCT.NAME, ORDER_LINE.QTY, ORDER_LINE.UNIT_PRICE)
								.from(ORDER_LINE)
								.join(PRODUCT).on(PRODUCT.ID.eq(ORDER_LINE.PRODUCT_ID))
								.where(ORDER_LINE.ORDER_ID.eq(ORDERS.ID))
								.orderBy(ORDER_LINE.PRODUCT_ID))
						.convertFrom(records -> records.map(record -> new OrderLineDetails(record.value1(),
								record.value2(), record.value3(), record.value4()))))
				.from(ORDERS)
				.where(ORDERS.CUSTOMER_ID.eq(customerId))
				.orderBy(ORDERS.PLACED_AT.desc(), ORDERS.ID.desc())
				.fetch(record -> new OrderDetails(record.value1(), OrderStatus.valueOf(record.value2()),
						record.value3(), record.value4()));
	}

}
