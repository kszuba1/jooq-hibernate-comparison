package io.github.kszuba1.jooq_hibernate_comparison.core.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Customer(UUID id, String email, String fullName, OffsetDateTime createdAt) {
}
