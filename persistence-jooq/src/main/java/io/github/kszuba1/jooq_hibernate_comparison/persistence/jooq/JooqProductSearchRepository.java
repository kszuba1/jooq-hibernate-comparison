package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import java.util.List;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductFilter;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductSummary;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ProductSearchRepository;
import org.jooq.Condition;
import org.jooq.DSLContext;

import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.PRODUCT;
import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.PRODUCT_TAG;
import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.TAG;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.selectOne;

public class JooqProductSearchRepository implements ProductSearchRepository {

	private final DSLContext dsl;

	public JooqProductSearchRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public List<ProductSummary> search(ProductFilter filter) {
		return dsl.select(PRODUCT.ID, PRODUCT.SKU, PRODUCT.NAME, PRODUCT.PRICE, PRODUCT.STOCK_QTY)
				.from(PRODUCT)
				.where(toCondition(filter))
				.orderBy(PRODUCT.NAME.asc(), PRODUCT.ID.asc())
				.fetch(record -> new ProductSummary(record.value1(), record.value2(), record.value3(),
						record.value4(), record.value5()));
	}

	private static Condition toCondition(ProductFilter filter) {
		Condition condition = noCondition();
		if (filter.nameContains() != null) {
			condition = condition.and(PRODUCT.NAME.containsIgnoreCase(filter.nameContains()));
		}
		if (filter.categoryId() != null) {
			condition = condition.and(PRODUCT.CATEGORY_ID.eq(filter.categoryId()));
		}
		if (filter.minPrice() != null) {
			condition = condition.and(PRODUCT.PRICE.ge(filter.minPrice()));
		}
		if (filter.maxPrice() != null) {
			condition = condition.and(PRODUCT.PRICE.le(filter.maxPrice()));
		}
		if (filter.inStockOnly()) {
			condition = condition.and(PRODUCT.STOCK_QTY.gt(0));
		}
		if (filter.taggedWith() != null) {
			condition = condition.and(exists(selectOne()
					.from(PRODUCT_TAG)
					.join(TAG).on(TAG.ID.eq(PRODUCT_TAG.TAG_ID))
					.where(PRODUCT_TAG.PRODUCT_ID.eq(PRODUCT.ID), TAG.NAME.eq(filter.taggedWith()))));
		}
		return condition;
	}

}
