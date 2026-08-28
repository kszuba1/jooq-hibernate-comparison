package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.CategoryTreeNode;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductRevenue;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ReportingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaEntityJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaWindow;

public class HibernateCriteriaReportingRepository implements ReportingRepository {

	private final EntityManagerFactory entityManagerFactory;

	public HibernateCriteriaReportingRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public List<ProductRevenue> topProductsPerCategory(int topN) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
			JpaCriteriaQuery<Tuple> ranked = cb.createTupleQuery();
			JpaRoot<ProductEntity> product = ranked.from(ProductEntity.class);
			JpaEntityJoin<ProductEntity, CategoryEntity> category = product.join(CategoryEntity.class);
			category.on(cb.equal(category.get("id"), product.get("categoryId")));
			JpaEntityJoin<ProductEntity, OrderLineEntity> line = product.join(OrderLineEntity.class, JoinType.LEFT);
			line.on(cb.equal(line.get("product"), product));

			JpaExpression<BigDecimal> revenue = cb.coalesce(
					cb.sum(cb.prod(line.get("qty").as(BigDecimal.class), line.<BigDecimal>get("unitPrice"))),
					cb.literal(BigDecimal.ZERO));
			JpaWindow window = cb.createWindow()
					.partitionBy(category.get("id"))
					.orderBy(cb.desc(revenue), cb.asc(product.get("id")));

			ranked.multiselect(
					category.get("id").alias("categoryId"),
					category.get("name").alias("categoryName"),
					product.get("id").alias("productId"),
					product.get("name").alias("productName"),
					revenue.alias("revenue"),
					cb.rank(window).alias("rnk"))
					.groupBy(category.get("id"), category.get("name"), product.get("id"), product.get("name"));

			JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			JpaCteCriteria<Tuple> cte = query.with(ranked);
			JpaRoot<Tuple> row = query.from(cte);
			query.multiselect(row.get("categoryId"), row.get("categoryName"), row.get("productId"),
					row.get("productName"), row.get("revenue"), row.get("rnk"))
					.where(cb.le(row.get("rnk"), topN))
					.orderBy(cb.asc(row.get("categoryName")), cb.asc(row.get("rnk")),
							cb.asc(row.get("productId")));

			return em.createQuery(query).getResultList().stream()
					.map(tuple -> new ProductRevenue(tuple.get(0, UUID.class), tuple.get(1, String.class),
							tuple.get(2, UUID.class), tuple.get(3, String.class),
							tuple.get(4, BigDecimal.class), ((Number) tuple.get(5)).intValue()))
					.toList();
		}
	}

	@Override
	public List<CategoryTreeNode> categoryTree(UUID rootId) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
			JpaCriteriaQuery<Tuple> base = cb.createTupleQuery();
			JpaRoot<CategoryEntity> root = base.from(CategoryEntity.class);
			base.multiselect(
					root.get("id").alias("id"),
					root.get("parentId").alias("parentId"),
					root.get("name").alias("name"),
					cb.literal(0).alias("depth"))
					.where(cb.equal(root.get("id"), rootId));

			JpaCriteriaQuery<Tuple> query = cb.createTupleQuery();
			JpaCteCriteria<Tuple> tree = query.withRecursiveUnionAll(base, self -> {
				JpaCriteriaQuery<Tuple> step = cb.createTupleQuery();
				JpaRoot<CategoryEntity> child = step.from(CategoryEntity.class);
				JpaRoot<Tuple> parent = step.from(self);
				return step.multiselect(
						child.get("id").alias("id"),
						child.get("parentId").alias("parentId"),
						child.get("name").alias("name"),
						cb.sum(parent.<Integer>get("depth"), 1).alias("depth"))
						.where(cb.equal(child.get("parentId"), parent.get("id")));
			});

			JpaRoot<Tuple> node = query.from(tree);
			query.multiselect(node.get("id"), node.get("parentId"), node.get("name"), node.get("depth"))
					.orderBy(cb.asc(node.get("depth")), cb.asc(node.get("name")));

			return em.createQuery(query).getResultList().stream()
					.map(tuple -> new CategoryTreeNode(tuple.get(0, UUID.class), tuple.get(1, UUID.class),
							tuple.get(2, String.class), ((Number) tuple.get(3)).intValue()))
					.toList();
		}
	}

}
