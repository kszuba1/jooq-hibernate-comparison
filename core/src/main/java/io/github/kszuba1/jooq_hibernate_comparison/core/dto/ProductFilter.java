package io.github.kszuba1.jooq_hibernate_comparison.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductFilter(String nameContains, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
		boolean inStockOnly, String taggedWith) {

	public static ProductFilter none() {
		return new ProductFilter(null, null, null, null, false, null);
	}

}
