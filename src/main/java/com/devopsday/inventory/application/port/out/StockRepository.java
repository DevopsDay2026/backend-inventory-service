package com.devopsday.inventory.application.port.out;

import com.devopsday.inventory.domain.model.StockItem;
import java.util.Optional;

public interface StockRepository {

  Optional<StockItem> findBySku(String sku);

  void save(StockItem item);
}
