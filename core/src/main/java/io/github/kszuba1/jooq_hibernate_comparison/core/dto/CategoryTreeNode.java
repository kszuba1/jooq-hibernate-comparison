package io.github.kszuba1.jooq_hibernate_comparison.core.dto;

import java.util.UUID;

public record CategoryTreeNode(UUID id, UUID parentId, String name, int depth) {
}
