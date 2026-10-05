package com.devopsday.inventory.application.usecase;

import com.devopsday.inventory.application.port.in.SetStockUseCase;
import com.devopsday.inventory.application.port.out.StockRepository;
import com.devopsday.inventory.domain.model.StockItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SetStockService implements SetStockUseCase {

  private final StockRepository stock;

  SetStockService(StockRepository stock) {
    this.stock = stock;
  }

  @Override
  @Transactional
  public StockItem set(Command command) {
    var item = StockItem.of(command.sku(), command.available());
    stock.save(item);
    return item;
  }
}
