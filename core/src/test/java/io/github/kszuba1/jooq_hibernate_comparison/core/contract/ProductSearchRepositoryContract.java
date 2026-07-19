package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductFilter;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductSummary;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ProductSearchRepositoryContract {

	private final UUID electronics = UUID.randomUUID();
	private final UUID furniture = UUID.randomUUID();
	private final UUID laptop = UUID.randomUUID();
	private final UUID phone = UUID.randomUUID();
	private final UUID monitor = UUID.randomUUID();
	private final UUID desk = UUID.randomUUID();
	private final UUID chair = UUID.randomUUID();
	private final UUID tee = UUID.randomUUID();
	private final UUID saleTag = UUID.randomUUID();
	private final UUID premiumTag = UUID.randomUUID();

	protected abstract ProductSearchRepository repository();

	protected abstract DataSource dataSource();

	@BeforeEach
	void seedCatalog() {
		DataSource ds = dataSource();
		JdbcFixtures.truncateAll(ds);
		JdbcFixtures.insertCategory(ds, electronics, null, "electronics");
		JdbcFixtures.insertCategory(ds, furniture, null, "furniture");
		JdbcFixtures.insertProduct(ds, laptop, electronics, "S7-LAP", "Pro Laptop", new BigDecimal("1500.00"), 5);
		JdbcFixtures.insertProduct(ds, phone, electronics, "S7-PHO", "Smart Phone", new BigDecimal("800.00"), 0);
		JdbcFixtures.insertProduct(ds, monitor, electronics, "S7-MON", "4K Monitor", new BigDecimal("400.00"), 12);
		JdbcFixtures.insertProduct(ds, desk, furniture, "S7-DES", "Standing Desk", new BigDecimal("600.00"), 3);
		JdbcFixtures.insertProduct(ds, chair, furniture, "S7-CHA", "Office Chair pro", new BigDecimal("250.00"), 0);
		JdbcFixtures.insertProduct(ds, tee, electronics, "S7-TEE", "100%_Cotton Tee", new BigDecimal("25.00"), 7);
		JdbcFixtures.insertTag(ds, saleTag, "sale");
		JdbcFixtures.insertTag(ds, premiumTag, "premium");
		JdbcFixtures.insertProductTag(ds, laptop, premiumTag);
		JdbcFixtures.insertProductTag(ds, phone, saleTag);
		JdbcFixtures.insertProductTag(ds, phone, premiumTag);
		JdbcFixtures.insertProductTag(ds, chair, saleTag);
	}

	@Test
	void allNullFilterReturnsEverythingOrderedByName() {
		List<ProductSummary> result = repository().search(ProductFilter.none());

		assertThat(result).extracting(ProductSummary::name)
				.containsExactly("100%_Cotton Tee", "4K Monitor", "Office Chair pro", "Pro Laptop", "Smart Phone",
						"Standing Desk");
	}

	@Test
	void nameFilterMatchesCaseInsensitivelyAnywhere() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter("PRO", null, null, null, false, null));

		assertThat(result).extracting(ProductSummary::id).containsExactlyInAnyOrder(laptop, chair);
	}

	@Test
	void categoryFilterRestrictsToThatCategory() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter(null, furniture, null, null, false, null));

		assertThat(result).extracting(ProductSummary::id).containsExactlyInAnyOrder(desk, chair);
	}

	@Test
	void priceBoundsAreInclusive() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter(null, null, new BigDecimal("400.00"), new BigDecimal("800.00"), false, null));

		assertThat(result).extracting(ProductSummary::id).containsExactlyInAnyOrder(monitor, desk, phone);
	}

	@Test
	void inStockOnlyExcludesZeroStock() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter(null, null, null, null, true, null));

		assertThat(result).extracting(ProductSummary::id).containsExactlyInAnyOrder(laptop, monitor, desk, tee);
	}

	@Test
	void minPriceAloneIsEnough() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter(null, null, new BigDecimal("600.00"), null, false, null));

		assertThat(result).extracting(ProductSummary::id).containsExactlyInAnyOrder(laptop, phone, desk);
	}

	@Test
	void maxPriceAloneIsEnough() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter(null, null, null, new BigDecimal("400.00"), false, null));

		assertThat(result).extracting(ProductSummary::id).containsExactlyInAnyOrder(monitor, chair, tee);
	}

	@Test
	void likeMetacharactersInNameFilterMatchLiterally() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter("%_", null, null, null, false, null));

		assertThat(result).extracting(ProductSummary::id).containsExactly(tee);
	}

	@Test
	void tagFilterMatchesExactTagName() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter(null, null, null, null, false, "sale"));

		assertThat(result).extracting(ProductSummary::id).containsExactlyInAnyOrder(phone, chair);
	}

	@Test
	void allFiltersCombineWithAnd() {
		List<ProductSummary> result = repository().search(new ProductFilter("o", electronics,
				new BigDecimal("100.00"), new BigDecimal("1000.00"), true, null));

		assertThat(result).extracting(ProductSummary::id).containsExactly(monitor);
	}

	@Test
	void combinationWithTagAndStock() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter(null, null, null, null, true, "premium"));

		assertThat(result).extracting(ProductSummary::id).containsExactly(laptop);
	}

	@Test
	void impossibleCombinationReturnsEmpty() {
		List<ProductSummary> result = repository().search(new ProductFilter("laptop", furniture, null, null,
				false, null));

		assertThat(result).isEmpty();
	}

	@Test
	void mapsAllSummaryFields() {
		List<ProductSummary> result = repository()
				.search(new ProductFilter("4K", null, null, null, false, null));

		assertThat(result).hasSize(1);
		ProductSummary summary = result.getFirst();
		assertThat(summary.id()).isEqualTo(monitor);
		assertThat(summary.sku()).isEqualTo("S7-MON");
		assertThat(summary.price()).isEqualByComparingTo(new BigDecimal("400.00"));
		assertThat(summary.stockQty()).isEqualTo(12);
	}

}
