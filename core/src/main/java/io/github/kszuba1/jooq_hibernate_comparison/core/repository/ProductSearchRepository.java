package io.github.kszuba1.jooq_hibernate_comparison.core.repository;

import java.util.List;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductFilter;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductSummary;

public interface ProductSearchRepository {

	List<ProductSummary> search(ProductFilter filter);

}
