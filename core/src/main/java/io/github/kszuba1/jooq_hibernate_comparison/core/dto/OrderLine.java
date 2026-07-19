package io.github.kszuba1.jooq_hibernate_comparison.core.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLine(UUID id, UUID productId, int quantity, BigDecimal unitPrice) {
}
