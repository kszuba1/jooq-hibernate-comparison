package io.github.kszuba1.jooq_hibernate_comparison.core.repository;

import java.util.UUID;

public class InsufficientStockException extends RuntimeException {

	public InsufficientStockException(UUID productId, int requested, int available) {
		super("insufficient stock for product " + productId + ": requested " + requested
				+ ", available " + available);
	}

}
