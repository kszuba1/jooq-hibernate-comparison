package io.github.kszuba1.jooq_hibernate_comparison.core.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDetails(UUID orderId, OrderStatus status, OffsetDateTime placedAt, List<OrderLineDetails> lines) {
}
