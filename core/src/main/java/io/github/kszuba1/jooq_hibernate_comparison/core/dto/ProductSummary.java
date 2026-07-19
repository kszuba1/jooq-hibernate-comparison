package io.github.kszuba1.jooq_hibernate_comparison.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummary(UUID id, String sku, String name, BigDecimal price, int stockQty) {
}
