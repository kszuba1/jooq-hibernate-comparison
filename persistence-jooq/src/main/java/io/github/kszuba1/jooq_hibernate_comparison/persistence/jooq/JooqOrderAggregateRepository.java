package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Order;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderLine;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderAggregateRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.tables.records.OrdersRecord;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;

import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.ORDERS;
import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.ORDER_LINE;

public class JooqOrderAggregateRepository implements OrderAggregateRepository {

	private final DSLContext dsl;
	private final boolean batchLineInserts;

	public JooqOrderAggregateRepository(DSLContext dsl) {
		this(dsl, false);
	}

	public JooqOrderAggregateRepository(DSLContext dsl, boolean batchLineInserts) {
		this.dsl = dsl;
		this.batchLineInserts = batchLineInserts;
	}

	@Override
	public void create(Order order) {
		dsl.transaction(tx -> {
			DSLContext ctx = tx.dsl();
			ctx.insertInto(ORDERS)
					.set(ORDERS.ID, order.id())
					.set(ORDERS.CUSTOMER_ID, order.customerId())
					.set(ORDERS.STATUS, order.status().name())
					.set(ORDERS.PLACED_AT, order.placedAt())
					.execute();
			if (batchLineInserts && !order.lines().isEmpty()) {
				BatchBindStep batch = ctx.batch(ctx
						.insertInto(ORDER_LINE, ORDER_LINE.ID, ORDER_LINE.ORDER_ID, ORDER_LINE.PRODUCT_ID,
								ORDER_LINE.QTY, ORDER_LINE.UNIT_PRICE)
						.values((UUID) null, null, null, null, null));
				for (OrderLine line : order.lines()) {
					batch = batch.bind(line.id(), order.id(), line.productId(), line.quantity(),
							line.unitPrice());
				}
				batch.execute();
			} else {
				for (OrderLine line : order.lines()) {
					ctx.insertInto(ORDER_LINE)
							.set(ORDER_LINE.ID, line.id())
							.set(ORDER_LINE.ORDER_ID, order.id())
							.set(ORDER_LINE.PRODUCT_ID, line.productId())
							.set(ORDER_LINE.QTY, line.quantity())
							.set(ORDER_LINE.UNIT_PRICE, line.unitPrice())
							.execute();
				}
			}
		});
	}

	@Override
	public Optional<Order> findById(UUID id) {
		OrdersRecord order = dsl.selectFrom(ORDERS).where(ORDERS.ID.eq(id)).fetchOne();
		if (order == null) {
			return Optional.empty();
		}
		List<OrderLine> lines = dsl.selectFrom(ORDER_LINE)
				.where(ORDER_LINE.ORDER_ID.eq(id))
				.orderBy(ORDER_LINE.PRODUCT_ID)
				.fetch(record -> new OrderLine(record.getId(), record.getProductId(), record.getQty(),
						record.getUnitPrice()));
		return Optional.of(new Order(order.getId(), order.getCustomerId(), OrderStatus.valueOf(order.getStatus()),
				order.getPlacedAt(), lines));
	}

	@Override
	public boolean deleteById(UUID id) {
		return dsl.transactionResult(tx -> {
			DSLContext ctx = tx.dsl();
			ctx.deleteFrom(ORDER_LINE).where(ORDER_LINE.ORDER_ID.eq(id)).execute();
			return ctx.deleteFrom(ORDERS).where(ORDERS.ID.eq(id)).execute() == 1;
		});
	}

}
