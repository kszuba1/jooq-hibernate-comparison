package io.github.kszuba1.jooq_hibernate_comparison.benchmarks;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.persistence.jooq.generated.Tables;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaPipelineTest {

	private static final Set<String> EXPECTED_TABLES = Set.of(
			"customer", "category", "product", "orders", "order_line", "review", "tag", "product_tag");

	@Autowired
	private DataSource dataSource;

	@Test
	void flywayAppliedTheInitialSchema() throws SQLException {
		Set<String> tables = new HashSet<>();
		try (Connection connection = dataSource.getConnection();
				ResultSet rs = connection.getMetaData().getTables(null, "public", null, new String[] { "TABLE" })) {
			while (rs.next()) {
				tables.add(rs.getString("TABLE_NAME"));
			}
		}
		assertThat(tables).containsAll(EXPECTED_TABLES);
	}

	@Test
	void jooqCodeWasGeneratedFromTheSameSchema() {
		assertThat(Tables.CUSTOMER.getName()).isEqualTo("customer");
		assertThat(Tables.ORDERS.getName()).isEqualTo("orders");
		assertThat(Tables.PRODUCT_TAG.getPrimaryKey().getFields()).hasSize(2);
	}

	@Test
	void generatedColumnTypesFollowTheProjectConventions() {
		assertThat(Tables.ORDER_LINE.PRODUCT_ID.getType()).isEqualTo(UUID.class);
		assertThat(Tables.PRODUCT.PRICE.getType()).isEqualTo(BigDecimal.class);
		assertThat(Tables.ORDERS.PLACED_AT.getType()).isEqualTo(OffsetDateTime.class);
	}

}
