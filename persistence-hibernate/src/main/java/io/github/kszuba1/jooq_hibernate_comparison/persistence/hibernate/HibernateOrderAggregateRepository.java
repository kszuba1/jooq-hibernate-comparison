package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Order;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderLine;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.OrderAggregateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class HibernateOrderAggregateRepository implements OrderAggregateRepository {

	private static final Comparator<UUID> PG_UUID_ORDER = Comparator
			.comparingLong((UUID u) -> u.getMostSignificantBits() ^ Long.MIN_VALUE)
			.thenComparingLong(u -> u.getLeastSignificantBits() ^ Long.MIN_VALUE);

	private final EntityManagerFactory entityManagerFactory;

	public HibernateOrderAggregateRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public void create(Order order) {
		Transactions.inTransaction(entityManagerFactory, em -> {
			OrderEntity entity = new OrderEntity(order.id(), order.customerId(), order.status(), order.placedAt());
			for (OrderLine line : order.lines()) {
				entity.addLine(new OrderLineEntity(line.id(),
						em.getReference(ProductEntity.class, line.productId()), line.quantity(), line.unitPrice()));
			}
			em.persist(entity);
			return null;
		});
	}

	@Override
	public Optional<Order> findById(UUID id) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			return Optional.ofNullable(em.find(OrderEntity.class, id))
					.map(HibernateOrderAggregateRepository::toDto);
		}
	}

	@Override
	public boolean deleteById(UUID id) {
		return Transactions.inTransaction(entityManagerFactory, em -> {
			OrderEntity entity = em.find(OrderEntity.class, id);
			if (entity == null) {
				return false;
			}
			em.remove(entity);
			return true;
		});
	}

	private static Order toDto(OrderEntity entity) {
		List<OrderLine> lines = entity.getLines().stream()
				.map(line -> new OrderLine(line.getId(), line.getProduct().getId(), line.getQty(),
						line.getUnitPrice()))
				.sorted(Comparator.comparing(OrderLine::productId, PG_UUID_ORDER))
				.toList();
		return new Order(entity.getId(), entity.getCustomerId(), entity.getStatus(), entity.getPlacedAt(), lines);
	}

}
