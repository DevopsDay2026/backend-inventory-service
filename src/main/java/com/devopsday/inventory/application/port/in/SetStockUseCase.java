package com.devopsday.inventory.application.port.in;

import com.devopsday.inventory.domain.model.StockItem;

public interface SetStockUseCase {

  StockItem set(Command command);

  record Command(String sku, int available) {}
}
