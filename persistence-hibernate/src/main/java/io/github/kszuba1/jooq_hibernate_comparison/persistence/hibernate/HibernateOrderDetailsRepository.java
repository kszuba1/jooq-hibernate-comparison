package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderDetails;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderLineDetails;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderDetailsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class HibernateOrderDetailsRepository implements OrderDetailsRepository {

	private static final Comparator<UUID> PG_UUID_ORDER = Comparator
			.comparingLong((UUID u) -> u.getMostSignificantBits() ^ Long.MIN_VALUE)
			.thenComparingLong(u -> u.getLeastSignificantBits() ^ Long.MIN_VALUE);

	private static final String QUERY = """
			select o from OrderEntity o
			left join fetch o.lines l
			left join fetch l.product
			where o.customerId = :customerId
			order by o.placedAt desc, o.id desc""";

	private final EntityManagerFactory entityManagerFactory;

	public HibernateOrderDetailsRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public List<OrderDetails> findDetailsByCustomer(UUID customerId) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			return em.createQuery(QUERY, OrderEntity.class)
					.setParameter("customerId", customerId)
					.getResultList()
					.stream()
					.map(HibernateOrderDetailsRepository::toDto)
					.toList();
		}
	}

	private static OrderDetails toDto(OrderEntity order) {
		List<OrderLineDetails> lines = order.getLines().stream()
				.map(line -> new OrderLineDetails(line.getProduct().getId(), line.getProduct().getName(),
						line.getQty(), line.getUnitPrice()))
				.sorted(Comparator.comparing(OrderLineDetails::productId, PG_UUID_ORDER))
				.toList();
		return new OrderDetails(order.getId(), order.getStatus(), order.getPlacedAt(), lines);
	}

}
