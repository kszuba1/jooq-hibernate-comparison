package io.github.kszuba1.jooq_hibernate_comparison.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineDetails(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
}
