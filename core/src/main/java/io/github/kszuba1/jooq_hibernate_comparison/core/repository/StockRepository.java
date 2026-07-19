package io.github.kszuba1.jooq_hibernate_comparison.core.repository;

import java.util.UUID;

public interface StockRepository {

	void decrementStock(UUID productId, int quantity);

}
