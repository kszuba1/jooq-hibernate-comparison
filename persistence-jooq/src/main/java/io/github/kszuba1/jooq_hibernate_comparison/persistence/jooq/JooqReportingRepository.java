package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.CategoryTreeNode;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductRevenue;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ReportingRepository;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.Record6;
import org.jooq.Table;

import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.CATEGORY;
import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.ORDER_LINE;
import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.PRODUCT;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.rank;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.NUMERIC;
import static org.jooq.impl.SQLDataType.UUID;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class JooqReportingRepository implements ReportingRepository {

	private final DSLContext dsl;

	public JooqReportingRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public List<ProductRevenue> topProductsPerCategory(int topN) {
		Field<BigDecimal> revenueExpr = coalesce(sum(ORDER_LINE.QTY.mul(ORDER_LINE.UNIT_PRICE)),
				inline(BigDecimal.ZERO));
		Field<BigDecimal> revenue = revenueExpr.as("revenue");
		Field<Integer> rnk = rank()
				.over(org.jooq.impl.DSL.partitionBy(CATEGORY.ID).orderBy(revenueExpr.desc(), PRODUCT.ID.asc()))
				.as("rnk");

		Table<Record6<UUID, String, UUID, String, BigDecimal, Integer>> ranked = select(
				CATEGORY.ID.as("category_id"), CATEGORY.NAME.as("category_name"),
				PRODUCT.ID.as("product_id"), PRODUCT.NAME.as("product_name"), revenue, rnk)
				.from(PRODUCT)
				.join(CATEGORY).on(CATEGORY.ID.eq(PRODUCT.CATEGORY_ID))
				.leftJoin(ORDER_LINE).on(ORDER_LINE.PRODUCT_ID.eq(PRODUCT.ID))
				.groupBy(CATEGORY.ID, CATEGORY.NAME, PRODUCT.ID, PRODUCT.NAME)
				.asTable("ranked");

		return dsl.selectFrom(ranked)
				.where(ranked.field("rnk", INTEGER).le(topN))
				.orderBy(ranked.field("category_name", VARCHAR), ranked.field("rnk", INTEGER),
						ranked.field("product_id", UUID))
				.fetch(record -> new ProductRevenue(record.value1(), record.value2(), record.value3(),
						record.value4(), record.value5(), record.value6()));
	}

	@Override
	public List<CategoryTreeNode> categoryTree(UUID rootId) {
		CommonTableExpression<Record4<UUID, UUID, String, Integer>> tree = name("tree")
				.fields("id", "parent_id", "name", "depth")
				.as(select(CATEGORY.ID, CATEGORY.PARENT_ID, CATEGORY.NAME, inline(0))
						.from(CATEGORY)
						.where(CATEGORY.ID.eq(rootId))
						.unionAll(select(CATEGORY.ID, CATEGORY.PARENT_ID, CATEGORY.NAME,
								field(name("tree", "depth"), INTEGER).plus(1))
								.from(CATEGORY)
								.join(table(name("tree")))
								.on(CATEGORY.PARENT_ID.eq(field(name("tree", "id"), UUID)))));

		return dsl.withRecursive(tree)
				.selectFrom(tree)
				.orderBy(tree.field("depth", INTEGER), tree.field("name", VARCHAR))
				.fetch(record -> new CategoryTreeNode(record.value1(), record.value2(), record.value3(),
						record.value4()));
	}

}
