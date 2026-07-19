package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.function.Function;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

final class Transactions {

	private Transactions() {
	}

	static <T> T inTransaction(EntityManagerFactory entityManagerFactory, Function<EntityManager, T> work) {
		try (EntityManager em = entityManagerFactory.createEntityManager()) {
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			try {
				T result = work.apply(em);
				transaction.commit();
				return result;
			} catch (RuntimeException | Error e) {
				if (transaction.isActive()) {
					transaction.rollback();
				}
				throw e;
			}
		}
	}

}
