package com.devopsday.inventory.adapter.out.persistence;

import com.devopsday.inventory.domain.model.StockItem;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class StockEntityMapper {

  public StockItem toDomain(StockEntity entity) {
    return StockItem.of(entity.sku, entity.available);
  }

  public StockEntity toNewEntity(StockItem item) {
    var entity = new StockEntity();
    entity.id = UUID.randomUUID();
    entity.sku = item.sku();
    applyState(item, entity);
    return entity;
  }

  public void applyState(StockItem item, StockEntity entity) {
    entity.available = item.available();
  }
}
