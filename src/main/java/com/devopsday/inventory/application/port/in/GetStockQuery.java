package com.devopsday.inventory.application.port.in;

import com.devopsday.inventory.domain.model.StockItem;

public interface GetStockQuery {

  StockItem bySku(String sku);
}
