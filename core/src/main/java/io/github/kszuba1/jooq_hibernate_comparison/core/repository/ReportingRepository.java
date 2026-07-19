package io.github.kszuba1.jooq_hibernate_comparison.core.repository;

import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.CategoryTreeNode;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductRevenue;

public interface ReportingRepository {

	List<ProductRevenue> topProductsPerCategory(int topN);

	List<CategoryTreeNode> categoryTree(UUID rootId);

}
