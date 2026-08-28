package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.CategoryTreeNode;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductRevenue;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ReportingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class HibernateHqlReportingRepository implements ReportingRepository {

	private static final String TOP_PRODUCTS_HQL = """
			with ranked as (
				select c.id as categoryId, c.name as categoryName,
				       p.id as productId, p.name as productName,
				       coalesce(sum(ol.qty * ol.unitPrice), 0bd) as revenue,
				       rank() over (partition by c.id
				                    order by coalesce(sum(ol.qty * ol.unitPrice), 0bd) desc, p.id asc) as rnk
				from ProductEntity p
				join CategoryEntity c on c.id = p.categoryId
				left join OrderLineEntity ol on ol.product = p
				group by c.id, c.name, p.id, p.name
			)
			select new io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductRevenue(
			           r.categoryId, r.categoryName, r.productId, r.productName,
			           r.revenue, cast(r.rnk as Integer))
			from ranked r
			where r.rnk <= :topN
			order by r.categoryName, r.rnk, r.productId""";

	private static final String CATEGORY_TREE_HQL = """
			with tree as (
				select c.id as id, c.parentId as parentId, c.name as name, 0 as depth
				from CategoryEntity c
				where c.id = :rootId
				union all
				select child.id as id, child.parentId as parentId, child.name as name, t.depth + 1 as depth
				from CategoryEntity child
				join tree t on child.parentId = t.id
			)
			select new io.github.kszuba1.jooq_hibernate_comparison.core.dto.CategoryTreeNode(
			           t.id, t.parentId, t.name, t.depth)
			from tree t
			order by t.depth, t.name""";

	private final EntityManagerFactory entityManagerFactory;

	public HibernateHqlReportingRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public List<ProductRevenue> topProductsPerCategory(int topN) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			return em.createQuery(TOP_PRODUCTS_HQL, ProductRevenue.class)
					.setParameter("topN", (long) topN)
					.getResultList();
		}
	}

	@Override
	public List<CategoryTreeNode> categoryTree(UUID rootId) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			return em.createQuery(CATEGORY_TREE_HQL, CategoryTreeNode.class)
					.setParameter("rootId", rootId)
					.getResultList();
		}
	}

}
