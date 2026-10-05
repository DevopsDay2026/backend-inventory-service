package com.devopsday.inventory.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StockPanacheRepository implements PanacheRepositoryBase<StockEntity, UUID> {

  public Optional<StockEntity> findBySku(String sku) {
    return find("sku", sku).singleResultOptional();
  }
}
