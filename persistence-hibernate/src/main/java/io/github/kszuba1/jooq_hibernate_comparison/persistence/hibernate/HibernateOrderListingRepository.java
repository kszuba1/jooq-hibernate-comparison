package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.List;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderSummary;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderListingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class HibernateOrderListingRepository implements OrderListingRepository {

	private static final String QUERY = """
			select new io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderSummary(
				o.id, o.customerId, o.status, o.placedAt)
			from OrderEntity o
			where o.status = :status
			order by o.placedAt desc, o.id desc""";

	private final EntityManagerFactory entityManagerFactory;

	public HibernateOrderListingRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public List<OrderSummary> findByStatus(OrderStatus status, int limit, int offset) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			return em.createQuery(QUERY, OrderSummary.class)
					.setParameter("status", status)
					.setFirstResult(offset)
					.setMaxResults(limit)
					.getResultList();
		}
	}

}
