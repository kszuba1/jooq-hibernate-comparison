package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.repository.InsufficientStockException;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.StockRepository;
import org.jooq.DSLContext;
import org.jooq.Record2;

import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.PRODUCT;

public class JooqStockRepository implements StockRepository {

	private final DSLContext dsl;

	public JooqStockRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public void decrementStock(UUID productId, int quantity) {
		while (true) {
			boolean updated = dsl.transactionResult(tx -> {
				DSLContext ctx = tx.dsl();
				Record2<Integer, Integer> current = ctx
						.select(PRODUCT.STOCK_QTY, PRODUCT.VERSION)
						.from(PRODUCT)
						.where(PRODUCT.ID.eq(productId))
						.fetchOne();
				if (current == null) {
					throw new IllegalStateException("product " + productId + " does not exist");
				}
				int stock = current.value1();
				int version = current.value2();
				if (stock < quantity) {
					throw new InsufficientStockException(productId, quantity, stock);
				}
				return ctx.update(PRODUCT)
						.set(PRODUCT.STOCK_QTY, stock - quantity)
						.set(PRODUCT.VERSION, version + 1)
						.where(PRODUCT.ID.eq(productId), PRODUCT.VERSION.eq(version))
						.execute() == 1;
			});
			if (updated) {
				return;
			}
		}
	}

}
