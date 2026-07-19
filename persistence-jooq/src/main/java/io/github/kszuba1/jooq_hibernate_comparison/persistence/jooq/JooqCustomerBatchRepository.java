package io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq;

import java.util.List;
import java.util.UUID;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.Customer;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.CustomerBatchRepository;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;

import static io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables.CUSTOMER;

public class JooqCustomerBatchRepository implements CustomerBatchRepository {

	private final DSLContext dsl;

	public JooqCustomerBatchRepository(DSLContext dsl) {
		this.dsl = dsl;
	}

	@Override
	public void insertAll(List<Customer> customers) {
		if (customers.isEmpty()) {
			return;
		}
		dsl.transaction(tx -> {
			BatchBindStep batch = tx.dsl().batch(tx.dsl()
					.insertInto(CUSTOMER, CUSTOMER.ID, CUSTOMER.EMAIL, CUSTOMER.FULL_NAME, CUSTOMER.CREATED_AT)
					.values((UUID) null, null, null, null));
			for (Customer customer : customers) {
				batch = batch.bind(customer.id(), customer.email(), customer.fullName(), customer.createdAt());
			}
			batch.execute();
		});
	}

	@Override
	public void updateAll(List<Customer> customers) {
		if (customers.isEmpty()) {
			return;
		}
		dsl.transaction(tx -> {
			BatchBindStep batch = tx.dsl().batch(tx.dsl()
					.update(CUSTOMER)
					.set(CUSTOMER.EMAIL, (String) null)
					.set(CUSTOMER.FULL_NAME, (String) null)
					.where(CUSTOMER.ID.eq((UUID) null)));
			for (Customer customer : customers) {
				batch = batch.bind(customer.email(), customer.fullName(), customer.id());
			}
			batch.execute();
		});
	}

}
