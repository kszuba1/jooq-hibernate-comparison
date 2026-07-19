package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductFilter;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductSummary;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ProductSearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class HibernateProductSearchRepository implements ProductSearchRepository {

	private final EntityManagerFactory entityManagerFactory;

	public HibernateProductSearchRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public List<ProductSummary> search(ProductFilter filter) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<ProductSummary> query = cb.createQuery(ProductSummary.class);
			Root<ProductEntity> product = query.from(ProductEntity.class);

			List<Predicate> predicates = new ArrayList<>();
			if (filter.nameContains() != null) {
				String escaped = filter.nameContains()
						.replace("!", "!!")
						.replace("%", "!%")
						.replace("_", "!_")
						.toLowerCase(Locale.ROOT);
				predicates.add(cb.like(cb.lower(product.get("name")), "%" + escaped + "%", '!'));
			}
			if (filter.categoryId() != null) {
				predicates.add(cb.equal(product.get("categoryId"), filter.categoryId()));
			}
			if (filter.minPrice() != null) {
				predicates.add(cb.greaterThanOrEqualTo(product.get("price"), filter.minPrice()));
			}
			if (filter.maxPrice() != null) {
				predicates.add(cb.lessThanOrEqualTo(product.get("price"), filter.maxPrice()));
			}
			if (filter.inStockOnly()) {
				predicates.add(cb.greaterThan(product.get("stockQty"), 0));
			}
			if (filter.taggedWith() != null) {
				Subquery<ProductTagEntity> subquery = query.subquery(ProductTagEntity.class);
				Root<ProductTagEntity> productTag = subquery.from(ProductTagEntity.class);
				subquery.select(productTag)
						.where(cb.equal(productTag.get("product"), product),
								cb.equal(productTag.get("tag").get("name"), filter.taggedWith()));
				predicates.add(cb.exists(subquery));
			}

			query.select(cb.construct(ProductSummary.class, product.get("id"), product.get("sku"),
							product.get("name"), product.get("price"), product.get("stockQty")))
					.where(predicates.toArray(Predicate[]::new))
					.orderBy(cb.asc(product.get("name")), cb.asc(product.get("id")));
			return em.createQuery(query).getResultList();
		}
	}

}
