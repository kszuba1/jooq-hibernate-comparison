package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import java.util.Optional;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerRepository;
import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.tables.records.CustomerRecord;
import org.jooq.DSLContext;

import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.CUSTOMER;

public class JooqCustomerRepository implements CustomerRepository {

	private final DSLContext dsl;

	public JooqCustomerRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public void create(Customer customer) {
		dsl.transaction(tx -> tx.dsl()
				.insertInto(CUSTOMER)
				.set(CUSTOMER.ID, customer.id())
				.set(CUSTOMER.EMAIL, customer.email())
				.set(CUSTOMER.FULL_NAME, customer.fullName())
				.set(CUSTOMER.CREATED_AT, customer.createdAt())
				.execute());
	}

	@Override
	public Optional<Customer> findById(UUID id) {
		return dsl.selectFrom(CUSTOMER)
				.where(CUSTOMER.ID.eq(id))
				.fetchOptional()
				.map(JooqCustomerRepository::toDto);
	}

	@Override
	public Optional<Customer> findByEmail(String email) {
		return dsl.selectFrom(CUSTOMER)
				.where(CUSTOMER.EMAIL.eq(email))
				.fetchOptional()
				.map(JooqCustomerRepository::toDto);
	}

	@Override
	public boolean update(Customer customer) {
		return dsl.transactionResult(tx -> tx.dsl()
				.update(CUSTOMER)
				.set(CUSTOMER.EMAIL, customer.email())
				.set(CUSTOMER.FULL_NAME, customer.fullName())
				.where(CUSTOMER.ID.eq(customer.id()))
				.execute() == 1);
	}

	@Override
	public boolean deleteById(UUID id) {
		return dsl.transactionResult(tx -> tx.dsl()
				.deleteFrom(CUSTOMER)
				.where(CUSTOMER.ID.eq(id))
				.execute() == 1);
	}

	private static Customer toDto(CustomerRecord record) {
		return new Customer(record.getId(), record.getEmail(), record.getFullName(), record.getCreatedAt());
	}

}
