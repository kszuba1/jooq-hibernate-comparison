package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.CategoryTreeNode;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductRevenue;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ReportingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class HibernateReportingRepository implements ReportingRepository {

	private static final String TOP_PRODUCTS_SQL = """
			select category_id, category_name, product_id, product_name, revenue, rnk from (
				select c.id as category_id, c.name as category_name,
				       p.id as product_id, p.name as product_name,
				       coalesce(sum(ol.qty * ol.unit_price), 0) as revenue,
				       rank() over (partition by c.id
				                    order by coalesce(sum(ol.qty * ol.unit_price), 0) desc, p.id) as rnk
				from product p
				join category c on c.id = p.category_id
				left join order_line ol on ol.product_id = p.id
				group by c.id, c.name, p.id, p.name
			) ranked
			where rnk <= :topN
			order by category_name, rnk, product_id""";

	private static final String CATEGORY_TREE_SQL = """
			with recursive tree (id, parent_id, name, depth) as (
				select c.id, c.parent_id, c.name, 0
				from category c
				where c.id = :rootId
				union all
				select child.id, child.parent_id, child.name, tree.depth + 1
				from category child
				join tree on child.parent_id = tree.id
			)
			select id, parent_id, name, depth from tree
			order by depth, name""";

	private final EntityManagerFactory entityManagerFactory;

	public HibernateReportingRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public List<ProductRevenue> topProductsPerCategory(int topN) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			@SuppressWarnings("unchecked")
			List<Object[]> rows = em.createNativeQuery(TOP_PRODUCTS_SQL, Object[].class)
					.setParameter("topN", topN)
					.getResultList();
			return rows.stream()
					.map(row -> new ProductRevenue((UUID) row[0], (String) row[1], (UUID) row[2],
							(String) row[3], (BigDecimal) row[4], ((Number) row[5]).intValue()))
					.toList();
		}
	}

	@Override
	public List<CategoryTreeNode> categoryTree(UUID rootId) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			@SuppressWarnings("unchecked")
			List<Object[]> rows = em.createNativeQuery(CATEGORY_TREE_SQL, Object[].class)
					.setParameter("rootId", rootId)
					.getResultList();
			return rows.stream()
					.map(row -> new CategoryTreeNode((UUID) row[0], (UUID) row[1], (String) row[2],
							((Number) row[3]).intValue()))
					.toList();
		}
	}

}
