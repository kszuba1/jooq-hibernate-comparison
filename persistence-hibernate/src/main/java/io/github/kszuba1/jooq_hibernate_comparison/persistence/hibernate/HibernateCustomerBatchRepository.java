package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.List;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerBatchRepository;
import jakarta.persistence.EntityManagerFactory;

public class HibernateCustomerBatchRepository implements CustomerBatchRepository {

	private static final int FLUSH_INTERVAL = 50;

	private final EntityManagerFactory entityManagerFactory;

	public HibernateCustomerBatchRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
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
			return null;
		});
	}

}
