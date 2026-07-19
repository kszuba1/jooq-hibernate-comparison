package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class CustomerBatchRepositoryContract {

	protected abstract CustomerBatchRepository repository();

	protected abstract DataSource dataSource();

	@BeforeEach
	void wipe() {
		JdbcFixtures.truncateAll(dataSource());
	}

	@Test
	void insertAllPersistsEveryRow() {
		repository().insertAll(newCustomers(200));

		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from customer")).isEqualTo(200);
	}

	@Test
	void insertAllWritesAllColumns() {
		Customer customer = new Customer(UUID.randomUUID(), "s3.columns@example.com", "Batch Person",
				OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS));
		repository().insertAll(List.of(customer));

		assertThat(JdbcFixtures.queryInt(dataSource(),
				"select count(*) from customer where id = ? and email = ? and full_name = ? and created_at = ?",
				customer.id(), customer.email(), customer.fullName(), customer.createdAt())).isEqualTo(1);
	}

	@Test
	void insertAllIsAtomicWhenARowInTheMiddleFails() {
		List<Customer> customers = newCustomers(100);
		Customer duplicateEmail = new Customer(UUID.randomUUID(), customers.get(10).email(), "Dup",
				OffsetDateTime.now(ZoneOffset.UTC));
		customers.add(50, duplicateEmail);

		assertThatThrownBy(() -> repository().insertAll(customers)).isInstanceOf(RuntimeException.class);

		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from customer")).isZero();
	}

	@Test
	void updateAllRewritesEmailAndFullName() {
		List<Customer> customers = newCustomers(50);
		repository().insertAll(customers);

		List<Customer> changed = customers.stream()
				.map(c -> new Customer(c.id(), "changed." + c.email(), "Changed Name", c.createdAt()))
				.toList();
		repository().updateAll(changed);

		assertThat(JdbcFixtures.queryInt(dataSource(),
				"select count(*) from customer where full_name = 'Changed Name' and email like 'changed.%'"))
				.isEqualTo(50);
	}

	@Test
	void updateAllIgnoresUnknownIds() {
		List<Customer> customers = newCustomers(3);
		repository().insertAll(customers);

		List<Customer> updates = new ArrayList<>();
		updates.add(new Customer(customers.getFirst().id(), "s3.updated@example.com", "Updated",
				customers.getFirst().createdAt()));
		updates.add(new Customer(UUID.randomUUID(), "s3.ghost@example.com", "Ghost",
				OffsetDateTime.now(ZoneOffset.UTC)));
		repository().updateAll(updates);

		assertThat(JdbcFixtures.queryInt(dataSource(), "select count(*) from customer")).isEqualTo(3);
		assertThat(JdbcFixtures.queryInt(dataSource(),
				"select count(*) from customer where email = 's3.updated@example.com'")).isEqualTo(1);
		assertThat(JdbcFixtures.queryInt(dataSource(),
				"select count(*) from customer where email = 's3.ghost@example.com'")).isZero();
	}

	@Test
	void updateAllIsAtomicWhenAnUpdateViolatesAConstraint() {
		List<Customer> customers = newCustomers(2);
		repository().insertAll(customers);

		List<Customer> conflicting = List.of(
				new Customer(customers.get(0).id(), "s3.same@example.com", "First", customers.get(0).createdAt()),
				new Customer(customers.get(1).id(), "s3.same@example.com", "Second", customers.get(1).createdAt()));

		assertThatThrownBy(() -> repository().updateAll(conflicting)).isInstanceOf(RuntimeException.class);

		assertThat(JdbcFixtures.queryInt(dataSource(),
				"select count(*) from customer where email = 's3.same@example.com'")).isZero();
		assertThat(JdbcFixtures.queryInt(dataSource(),
				"select count(*) from customer where email = ?", customers.get(0).email())).isEqualTo(1);
	}

	private static List<Customer> newCustomers(int count) {
		List<Customer> customers = new ArrayList<>();
		OffsetDateTime createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
		for (int i = 0; i < count; i++) {
			customers.add(new Customer(UUID.randomUUID(), "s3." + i + "@example.com", "Batch Person " + i,
					createdAt));
		}
		return customers;
	}

}
