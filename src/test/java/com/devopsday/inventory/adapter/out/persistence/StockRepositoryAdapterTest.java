package com.devopsday.inventory.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.devopsday.inventory.domain.model.StockItem;
import com.devopsday.inventory.testsupport.InMemoryMessagingProfile;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(InMemoryMessagingProfile.class)
class StockRepositoryAdapterTest {

  @Inject StockRepositoryAdapter adapter;
  @Inject StockPanacheRepository panache;

  @Test
  @TestTransaction
  void savesAndReloadsStock() {
    var sku = uniqueSku();

    adapter.save(StockItem.of(sku, 7));
    flushAndClear();

    assertThat(adapter.findBySku(sku)).contains(StockItem.of(sku, 7));
  }

  @Test
  @TestTransaction
  void returnsEmptyForUnknownSku() {
    assertThat(adapter.findBySku(uniqueSku())).isEmpty();
  }

  @Test
  @TestTransaction
  void updatesAvailableOfExistingSkuAndBumpsVersion() {
    var sku = uniqueSku();
    adapter.save(StockItem.of(sku, 7));
    flushAndClear();
    var initialId = entityOf(sku).id;
    var initialVersion = entityOf(sku).version;

    adapter.save(StockItem.of(sku, 4));
    flushAndClear();

    assertThat(adapter.findBySku(sku)).contains(StockItem.of(sku, 4));
    var updated = entityOf(sku);
    assertThat(updated.id).isEqualTo(initialId);
    assertThat(updated.version).isGreaterThan(initialVersion);
    assertThat(panache.count("sku", sku)).isEqualTo(1);
  }

  @Test
  @TestTransaction
  void skuIsUnique() {
    var sku = uniqueSku();
    panache.persist(entity(sku, 1));
    panache.flush();

    var failure =
        catchThrowable(
            () -> {
              panache.persist(entity(sku, 2));
              panache.flush();
            });

    assertThat(failure).isInstanceOf(PersistenceException.class);
  }

  @Test
  void concurrentUpdateSurfacesAsOptimisticLockInsideTheCall() {
    var sku = uniqueSku();
    QuarkusTransaction.requiringNew().run(() -> adapter.save(StockItem.of(sku, 10)));

    var failure =
        catchThrowable(
            () ->
                QuarkusTransaction.requiringNew()
                    .run(
                        () -> {
                          adapter.findBySku(sku);
                          QuarkusTransaction.requiringNew()
                              .run(() -> adapter.save(StockItem.of(sku, 9)));
                          adapter.save(StockItem.of(sku, 8));
                        }));

    assertThat(causeChainOf(failure)).anyMatch(OptimisticLockException.class::isInstance);
    var stored = QuarkusTransaction.requiringNew().call(() -> adapter.findBySku(sku));
    assertThat(stored).contains(StockItem.of(sku, 9));
  }

  private StockEntity entityOf(String sku) {
    return panache.find("sku", sku).firstResult();
  }

  private static StockEntity entity(String sku, int available) {
    var entity = new StockEntity();
    entity.id = UUID.randomUUID();
    entity.sku = sku;
    entity.available = available;
    return entity;
  }

  private static String uniqueSku() {
    return "SKU-" + UUID.randomUUID();
  }

  private static Iterable<Throwable> causeChainOf(Throwable failure) {
    var chain = new ArrayList<Throwable>();
    for (var current = failure; current != null; current = current.getCause()) {
      chain.add(current);
    }
    return chain;
  }

  private void flushAndClear() {
    panache.flush();
    panache.getEntityManager().clear();
  }
}
