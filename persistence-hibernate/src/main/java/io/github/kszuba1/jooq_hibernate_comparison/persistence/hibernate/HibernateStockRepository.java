package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.repository.InsufficientStockException;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.StockRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;

public class HibernateStockRepository implements StockRepository {

	private final EntityManagerFactory entityManagerFactory;

	public HibernateStockRepository(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public void decrementStock(UUID productId, int quantity) {
		while (true) {
			try {
				attempt(productId, quantity);
				return;
			} catch (RuntimeException e) {
				if (!isOptimisticConflict(e)) {
					throw e;
				}
			}
		}
	}

	private void attempt(UUID productId, int quantity) {
		Transactions.inTransaction(entityManagerFactory, em -> {
			ProductEntity product = em.find(ProductEntity.class, productId);
			if (product == null) {
				throw new EntityNotFoundException("product " + productId + " does not exist");
			}
			if (product.getStockQty() < quantity) {
				throw new InsufficientStockException(productId, quantity, product.getStockQty());
			}
			product.setStockQty(product.getStockQty() - quantity);
			return null;
		});
	}

	private static boolean isOptimisticConflict(Throwable e) {
		for (Throwable current = e; current != null; current = current.getCause()) {
			if (current instanceof OptimisticLockException || current instanceof StaleStateException) {
				return true;
			}
		}
		return false;
	}

}
