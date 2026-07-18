package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class CustomerRepositoryContract {

	protected abstract CustomerRepository repository();

	protected abstract DataSource dataSource();

	@BeforeEach
	void wipeCustomerTable() throws SQLException {
		try (Connection connection = dataSource().getConnection();
				Statement statement = connection.createStatement()) {
			statement.execute("truncate table customer cascade");
		}
	}

	@Test
	void createThenFindByIdRoundTrips() {
		Customer customer = newCustomer("s1.roundtrip@example.com");
		repository().create(customer);

		Optional<Customer> found = repository().findById(customer.id());

		assertThat(found).isPresent();
		assertMatches(found.orElseThrow(), customer);
	}

	@Test
	void createIsVisibleToPlainJdbc() throws SQLException {
		Customer customer = newCustomer("s1.jdbc@example.com");
		repository().create(customer);

		assertThat(countById(customer.id())).isEqualTo(1);
	}

	@Test
	void findByIdReturnsEmptyForUnknownId() {
		assertThat(repository().findById(UUID.randomUUID())).isEmpty();
	}

	@Test
	void findByEmailFindsTheMatchingRow() {
		Customer customer = newCustomer("s1.byemail@example.com");
		repository().create(customer);
		repository().create(newCustomer("s1.other@example.com"));

		Optional<Customer> found = repository().findByEmail("s1.byemail@example.com");

		assertThat(found).isPresent();
		assertMatches(found.orElseThrow(), customer);
	}

	@Test
	void findByEmailReturnsEmptyForUnknownEmail() {
		assertThat(repository().findByEmail("s1.nobody@example.com")).isEmpty();
	}

	@Test
	void updateChangesEmailAndFullNameButNotCreatedAt() {
		Customer original = newCustomer("s1.before@example.com");
		repository().create(original);

		Customer changed = new Customer(original.id(), "s1.after@example.com", "Renamed Person",
				original.createdAt().plusDays(30));
		boolean updated = repository().update(changed);

		assertThat(updated).isTrue();
		Customer reloaded = repository().findById(original.id()).orElseThrow();
		assertThat(reloaded.email()).isEqualTo("s1.after@example.com");
		assertThat(reloaded.fullName()).isEqualTo("Renamed Person");
		assertThat(reloaded.createdAt().toInstant()).isEqualTo(original.createdAt().toInstant());
	}

	@Test
	void updateReturnsFalseForMissingRow() {
		assertThat(repository().update(newCustomer("s1.ghost@example.com"))).isFalse();
	}

	@Test
	void deleteRemovesTheRow() throws SQLException {
		Customer customer = newCustomer("s1.delete@example.com");
		repository().create(customer);

		boolean deleted = repository().deleteById(customer.id());

		assertThat(deleted).isTrue();
		assertThat(repository().findById(customer.id())).isEmpty();
		assertThat(countById(customer.id())).isZero();
	}

	@Test
	void deleteReturnsFalseForMissingRow() {
		assertThat(repository().deleteById(UUID.randomUUID())).isFalse();
	}

	@Test
	void duplicateIdIsRejected() {
		Customer customer = newCustomer("s1.dupid.a@example.com");
		repository().create(customer);
		Customer sameId = new Customer(customer.id(), "s1.dupid.b@example.com", "Someone Else",
				customer.createdAt());

		assertThatThrownBy(() -> repository().create(sameId)).isInstanceOf(RuntimeException.class);
	}

	@Test
	void duplicateEmailIsRejected() {
		repository().create(newCustomer("s1.dup@example.com"));

		assertThatThrownBy(() -> repository().create(newCustomer("s1.dup@example.com")))
				.isInstanceOf(RuntimeException.class);
	}

	private static Customer newCustomer(String email) {
		return new Customer(UUID.randomUUID(), email, "Jane Doe",
				OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS));
	}

	private static void assertMatches(Customer actual, Customer expected) {
		assertThat(actual.id()).isEqualTo(expected.id());
		assertThat(actual.email()).isEqualTo(expected.email());
		assertThat(actual.fullName()).isEqualTo(expected.fullName());
		assertThat(actual.createdAt().toInstant()).isEqualTo(expected.createdAt().toInstant());
	}

	private int countById(UUID id) throws SQLException {
		try (Connection connection = dataSource().getConnection();
				PreparedStatement statement = connection.prepareStatement("select count(*) from customer where id = ?")) {
			statement.setObject(1, id);
			try (ResultSet rs = statement.executeQuery()) {
				rs.next();
				return rs.getInt(1);
			}
		}
	}

}
