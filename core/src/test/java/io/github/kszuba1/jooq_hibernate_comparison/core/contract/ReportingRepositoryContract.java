package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.dto.CategoryTreeNode;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.OrderStatus;
import io.github.kszuba1.jooq_hibernate_comparison.core.dto.ProductRevenue;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.ReportingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class ReportingRepositoryContract {

	private final UUID electronics = UUID.randomUUID();
	private final UUID furniture = UUID.randomUUID();
	private final UUID laptop = UUID.randomUUID();
	private final UUID phone = UUID.randomUUID();
	private final UUID desk = UUID.randomUUID();
	private final UUID chair = UUID.randomUUID();
	private final UUID stool = UUID.randomUUID();

	private final UUID root = UUID.randomUUID();
	private final UUID midElectronics = UUID.randomUUID();
	private final UUID audio = UUID.randomUUID();
	private final UUID video = UUID.randomUUID();
	private final UUID otherRoot = UUID.randomUUID();

	protected abstract ReportingRepository repository();

	protected abstract DataSource dataSource();

	@BeforeEach
	void seedRevenueAndTree() {
		DataSource ds = dataSource();
		JdbcFixtures.truncateAll(ds);

		JdbcFixtures.insertCategory(ds, electronics, null, "electronics");
		JdbcFixtures.insertCategory(ds, furniture, null, "furniture");
		JdbcFixtures.insertProduct(ds, laptop, electronics, "S6-LAP", "Laptop", new BigDecimal("1000.00"), 10);
		JdbcFixtures.insertProduct(ds, phone, electronics, "S6-PHO", "Phone", new BigDecimal("500.00"), 10);
		JdbcFixtures.insertProduct(ds, desk, furniture, "S6-DES", "Desk", new BigDecimal("300.00"), 10);
		JdbcFixtures.insertProduct(ds, chair, furniture, "S6-CHA", "Chair", new BigDecimal("100.00"), 10);
		JdbcFixtures.insertProduct(ds, stool, furniture, "S6-STO", "Stool", new BigDecimal("50.00"), 10);

		UUID customer = UUID.randomUUID();
		JdbcFixtures.insertCustomer(ds, customer, "s6.buyer@example.com");
		OffsetDateTime placedAt = OffsetDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC);

		UUID orderA = UUID.randomUUID();
		JdbcFixtures.insertOrder(ds, orderA, customer, OrderStatus.PAID, placedAt);
		JdbcFixtures.insertOrderLine(ds, UUID.randomUUID(), orderA, laptop, 2, new BigDecimal("1000.00"));
		JdbcFixtures.insertOrderLine(ds, UUID.randomUUID(), orderA, phone, 4, new BigDecimal("500.00"));

		UUID orderB = UUID.randomUUID();
		JdbcFixtures.insertOrder(ds, orderB, customer, OrderStatus.SHIPPED, placedAt.plusDays(1));
		JdbcFixtures.insertOrderLine(ds, UUID.randomUUID(), orderB, laptop, 1, new BigDecimal("900.00"));
		JdbcFixtures.insertOrderLine(ds, UUID.randomUUID(), orderB, desk, 3, new BigDecimal("300.00"));

		JdbcFixtures.insertCategory(ds, root, null, "s6-root");
		JdbcFixtures.insertCategory(ds, midElectronics, root, "mid-electronics");
		JdbcFixtures.insertCategory(ds, audio, midElectronics, "audio");
		JdbcFixtures.insertCategory(ds, video, midElectronics, "video");
		JdbcFixtures.insertCategory(ds, otherRoot, null, "s6-other-root");
	}

	@Test
	void top1KeepsOnlyTheBestProductPerCategory() {
		List<ProductRevenue> result = repository().topProductsPerCategory(1);

		assertThat(result).hasSize(2);
		assertThat(result.get(0).categoryName()).isEqualTo("electronics");
		assertThat(result.get(0).productId()).isEqualTo(laptop);
		assertThat(result.get(0).revenue()).isEqualByComparingTo(new BigDecimal("2900.00"));
		assertThat(result.get(0).rank()).isEqualTo(1);
		assertThat(result.get(1).categoryName()).isEqualTo("furniture");
		assertThat(result.get(1).productId()).isEqualTo(desk);
		assertThat(result.get(1).revenue()).isEqualByComparingTo(new BigDecimal("900.00"));
	}

	@Test
	void top2RanksWithinEachCategoryByRevenue() {
		List<ProductRevenue> result = repository().topProductsPerCategory(2);

		UUID zeroRevenueWinner = tieWinner();
		assertThat(result).hasSize(4);
		assertThat(result).extracting(ProductRevenue::productId)
				.containsExactly(laptop, phone, desk, zeroRevenueWinner);
		assertThat(result).extracting(ProductRevenue::rank).containsExactly(1, 2, 1, 2);
	}

	@Test
	void revenueTieBreaksByProductIdInDatabaseOrder() {
		List<ProductRevenue> result = repository().topProductsPerCategory(3);

		List<ProductRevenue> furnitureRows = result.stream()
				.filter(r -> r.categoryName().equals("furniture")).toList();
		UUID winner = tieWinner();
		UUID loser = winner.equals(chair) ? stool : chair;
		assertThat(furnitureRows).extracting(ProductRevenue::productId).containsExactly(desk, winner, loser);
		assertThat(furnitureRows).extracting(ProductRevenue::rank).containsExactly(1, 2, 3);
	}

	@Test
	void productsWithoutSalesAppearWithZeroRevenue() {
		List<ProductRevenue> result = repository().topProductsPerCategory(2);

		ProductRevenue zeroRow = result.stream().filter(r -> r.productId().equals(tieWinner())).findFirst()
				.orElseThrow();
		assertThat(zeroRow.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(zeroRow.rank()).isEqualTo(2);
	}

	@Test
	void largeTopNReturnsEverythingStillRanked() {
		List<ProductRevenue> result = repository().topProductsPerCategory(100);

		assertThat(result).hasSize(5);
	}

	private UUID tieWinner() {
		return JdbcFixtures.PG_UUID_ORDER.compare(chair, stool) < 0 ? chair : stool;
	}

	@Test
	void categoryTreeReturnsTheWholeSubtreeWithDepths() {
		List<CategoryTreeNode> result = repository().categoryTree(root);

		assertThat(result).hasSize(4);
		assertThat(result.get(0).id()).isEqualTo(root);
		assertThat(result.get(0).depth()).isZero();
		assertThat(result.get(0).parentId()).isNull();
		assertThat(result.get(1).id()).isEqualTo(midElectronics);
		assertThat(result.get(1).depth()).isEqualTo(1);
		assertThat(result).filteredOn(n -> n.depth() == 2).extracting(CategoryTreeNode::name)
				.containsExactly("audio", "video");
	}

	@Test
	void categoryTreeOfALeafIsJustTheLeaf() {
		List<CategoryTreeNode> result = repository().categoryTree(audio);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().id()).isEqualTo(audio);
		assertThat(result.getFirst().depth()).isZero();
	}

	@Test
	void categoryTreeNeverLeaksSiblingRoots() {
		List<CategoryTreeNode> result = repository().categoryTree(root);

		assertThat(result).extracting(CategoryTreeNode::id).doesNotContain(otherRoot, electronics, furniture);
	}

	@Test
	void categoryTreeOfUnknownRootIsEmpty() {
		assertThat(repository().categoryTree(UUID.randomUUID())).isEmpty();
	}

}
