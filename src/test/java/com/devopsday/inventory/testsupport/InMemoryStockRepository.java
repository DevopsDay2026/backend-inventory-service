package com.devopsday.inventory.testsupport;

import com.devopsday.inventory.application.port.out.StockRepository;
import com.devopsday.inventory.domain.model.StockItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryStockRepository implements StockRepository {

  private final Map<String, StockItem> store = new HashMap<>();
  private final List<StockItem> saves = new ArrayList<>();

  @Override
  public Optional<StockItem> findBySku(String sku) {
    return Optional.ofNullable(store.get(sku));
  }

  @Override
  public void save(StockItem item) {
    store.put(item.sku(), item);
    saves.add(item);
  }

  public InMemoryStockRepository withStock(String sku, int available) {
    store.put(sku, StockItem.of(sku, available));
    return this;
  }

  public int availableOf(String sku) {
    return store.get(sku).available();
  }

  public int saveCount() {
    return saves.size();
  }
}
