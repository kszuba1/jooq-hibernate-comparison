package io.github.kszuba1.jooq_hibernate_comparison.core.repository;

import java.util.Optional;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;

public interface CustomerRepository {

	void create(Customer customer);

	Optional<Customer> findById(UUID id);

	Optional<Customer> findByEmail(String email);

	boolean update(Customer customer);

	boolean deleteById(UUID id);

}
