package io.github.kszuba1.jooq_hibernate_comparison.core.repository;

import java.util.List;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;

public interface CustomerBatchRepository {

	void insertAll(List<Customer> customers);

	void updateAll(List<Customer> customers);

}
