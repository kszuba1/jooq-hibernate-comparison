package io.github.kszuba1.jooq_hibernate_comparison.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRevenue(UUID categoryId, String categoryName, UUID productId, String productName,
		BigDecimal revenue, int rank) {
}
