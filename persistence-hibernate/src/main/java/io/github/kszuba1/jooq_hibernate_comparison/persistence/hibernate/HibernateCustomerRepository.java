package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

public class HibernateCustomerRepository implements CustomerRepository {

	private final EntityManagerFactory entityManagerFactory;

	public HibernateCustomerRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public void create(Customer customer) {
		inTransaction(em -> {
			em.persist(new CustomerEntity(customer.id(), customer.email(), customer.fullName(),
					customer.createdAt()));
			return null;
		});
	}

	@Override
	public Optional<Customer> findById(UUID id) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			return Optional.ofNullable(em.find(CustomerEntity.class, id))
					.map(HibernateCustomerRepository::toDto);
		}
	}

	@Override
	public Optional<Customer> findByEmail(String email) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			return em.createQuery("select c from CustomerEntity c where c.email = :email", CustomerEntity.class)
					.setParameter("email", email)
					.getResultStream()
					.findFirst()
					.map(HibernateCustomerRepository::toDto);
		}
	}

	@Override
	public boolean update(Customer customer) {
		return inTransaction(em -> {
			CustomerEntity entity = em.find(CustomerEntity.class, customer.id());
			if (entity == null) {
				return false;
			}
			entity.setEmail(customer.email());
			entity.setFullName(customer.fullName());
			return true;
		});
	}

	@Override
	public boolean deleteById(UUID id) {
		return inTransaction(em -> {
			CustomerEntity entity = em.find(CustomerEntity.class, id);
			if (entity == null) {
				return false;
			}
			em.remove(entity);
			return true;
		});
	}

	private <T> T inTransaction(Function<EntityManager, T> work) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			try {
				T result = work.apply(em);
				transaction.commit();
				return result;
			} catch (RuntimeException e) {
				if (transaction.isActive()) {
					transaction.rollback();
				}
				throw e;
			}
		}
	}

	private static Customer toDto(CustomerEntity entity) {
		return new Customer(entity.getId(), entity.getEmail(), entity.getFullName(), entity.getCreatedAt());
	}

}
