package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerBatchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;

public class HibernateCustomerBatchRepository implements CustomerBatchRepository {

	private static final int FLUSH_INTERVAL = 50;

	private final EntityManagerFactory entityManagerFactory;

	private final boolean multiLoad;

	public HibernateCustomerBatchRepository(EntityManagerFactory entityManagerFactory) {
		this(entityManagerFactory, false);
	}

	public HibernateCustomerBatchRepository(EntityManagerFactory entityManagerFactory, boolean multiLoad) {
		this.entityManagerFactory = entityManagerFactory;
		this.multiLoad = multiLoad;
	}

	@Override
	public void insertAll(List<Customer> customers) {
		Transactions.inTransaction(entityManagerFactory, em -> {
			int i = 0;
			for (Customer customer : customers) {
				em.persist(new CustomerEntity(customer.id(), customer.email(), customer.fullName(),
						customer.createdAt()));
				if (++i % FLUSH_INTERVAL == 0) {
					em.flush();
					em.clear();
				}
			}
			return null;
		});
	}

	@Override
	public void updateAll(List<Customer> customers) {
		Transactions.inTransaction(entityManagerFactory, em -> {
			if (multiLoad) {
				updateAllByMultiLoad(em, customers);
			}
			else {
				updateAllByFind(em, customers);
			}
			return null;
		});
	}

	private void updateAllByFind(EntityManager em, List<Customer> customers) {
		int i = 0;
		for (Customer customer : customers) {
			CustomerEntity entity = em.find(CustomerEntity.class, customer.id());
			if (entity != null) {
				entity.setEmail(customer.email());
				entity.setFullName(customer.fullName());
			}
			if (++i % FLUSH_INTERVAL == 0) {
				em.flush();
				em.clear();
			}
		}
	}

	private void updateAllByMultiLoad(EntityManager em, List<Customer> customers) {
		Session session = em.unwrap(Session.class);
		for (int start = 0; start < customers.size(); start += FLUSH_INTERVAL) {
			List<Customer> window = customers.subList(start,
					Math.min(start + FLUSH_INTERVAL, customers.size()));
			List<UUID> ids = window.stream().map(Customer::id).toList();
			List<CustomerEntity> entities = session.byMultipleIds(CustomerEntity.class)
					.enableOrderedReturn(true)
					.withBatchSize(window.size())
					.multiLoad(ids);
			for (int i = 0; i < window.size(); i++) {
				CustomerEntity entity = entities.get(i);
				if (entity != null) {
					entity.setEmail(window.get(i).email());
					entity.setFullName(window.get(i).fullName());
				}
			}
			em.flush();
			em.clear();
		}
	}

}
