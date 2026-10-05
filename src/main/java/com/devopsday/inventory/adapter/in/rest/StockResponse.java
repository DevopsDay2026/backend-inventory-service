package com.devopsday.inventory.adapter.in.rest;

import com.devopsday.inventory.domain.model.StockItem;

public record StockResponse(String sku, int available) {

  public static StockResponse from(StockItem item) {
    return new StockResponse(item.sku(), item.available());
  }
}
