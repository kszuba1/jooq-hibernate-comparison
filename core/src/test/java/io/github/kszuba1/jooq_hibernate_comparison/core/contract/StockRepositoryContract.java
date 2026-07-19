package io.github.kszuba1.jooq_hibernate_comparison.core.contract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import io.github.kszuba1.jooq_hibernate_comparison.core.repository.InsufficientStockException;
import io.github.kszuba1.jooq_hibernate_comparison.core.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class StockRepositoryContract {

	private static final int THREADS = 8;
	private static final int DECREMENTS_PER_THREAD = 25;

	private final UUID categoryId = UUID.randomUUID();
	private final UUID productId = UUID.randomUUID();

	protected abstract StockRepository repository();

	protected abstract DataSource dataSource();

	@BeforeEach
	void seedProduct() {
		JdbcFixtures.truncateAll(dataSource());
		JdbcFixtures.insertCategory(dataSource(), categoryId, null, "s8-category");
		JdbcFixtures.insertProduct(dataSource(), productId, categoryId, "S8-HOT", "Hot Product",
				new BigDecimal("9.99"), 1000);
	}

	@Test
	void decrementReducesStock() {
		repository().decrementStock(productId, 7);

		assertThat(stock()).isEqualTo(993);
		assertThat(version()).isEqualTo(1);
	}

	@Test
	void decrementToExactlyZeroIsAllowed() {
		repository().decrementStock(productId, 1000);

		assertThat(stock()).isZero();
	}

	@Test
	void insufficientStockThrowsAndLeavesRowUntouched() {
		assertThatThrownBy(() -> repository().decrementStock(productId, 1001))
				.isInstanceOf(InsufficientStockException.class);

		assertThat(stock()).isEqualTo(1000);
		assertThat(version()).isZero();
	}

	@Test
	void unknownProductThrows() {
		assertThatThrownBy(() -> repository().decrementStock(UUID.randomUUID(), 1))
				.isInstanceOf(RuntimeException.class);
	}

	@Test
	@org.junit.jupiter.api.Timeout(120)
	void concurrentDecrementsLoseNoUpdates() throws Exception {
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger failures = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
			for (int t = 0; t < THREADS; t++) {
				futures.add(executor.submit(() -> {
					start.await();
					for (int i = 0; i < DECREMENTS_PER_THREAD; i++) {
						try {
							repository().decrementStock(productId, 1);
						} catch (RuntimeException e) {
							failures.incrementAndGet();
						}
					}
					return null;
				}));
			}
			start.countDown();
			for (Future<?> future : futures) {
				future.get();
			}
		}

		assertThat(failures).hasValue(0);
		int expected = 1000 - THREADS * DECREMENTS_PER_THREAD;
		assertThat(stock()).isEqualTo(expected);
		assertThat(version()).isEqualTo(THREADS * DECREMENTS_PER_THREAD);
	}

	private int stock() {
		return JdbcFixtures.queryInt(dataSource(), "select stock_qty from product where id = ?", productId);
	}

	private int version() {
		return JdbcFixtures.queryInt(dataSource(), "select version from product where id = ?", productId);
	}

}
