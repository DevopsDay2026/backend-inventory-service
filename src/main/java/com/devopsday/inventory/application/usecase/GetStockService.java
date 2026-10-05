package com.devopsday.inventory.application.usecase;

import com.devopsday.inventory.application.port.in.GetStockQuery;
import com.devopsday.inventory.application.port.out.StockRepository;
import com.devopsday.inventory.domain.exception.StockNotFound;
import com.devopsday.inventory.domain.model.StockItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GetStockService implements GetStockQuery {

  private final StockRepository stock;

  GetStockService(StockRepository stock) {
    this.stock = stock;
  }

  @Override
  @Transactional
  public StockItem bySku(String sku) {
    return stock.findBySku(sku).orElseThrow(() -> new StockNotFound(sku));
  }
}
