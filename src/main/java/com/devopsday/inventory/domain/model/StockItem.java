package com.devopsday.inventory.domain.model;

import com.devopsday.inventory.domain.exception.InvalidStock;

public record StockItem(String sku, int available) {

  public StockItem {
    if (sku == null || sku.isBlank()) {
      throw new InvalidStock("sku is required");
    }
    if (available < 0) {
      throw new InvalidStock("available must not be negative for sku " + sku);
    }
  }

  public static StockItem of(String sku, int available) {
    return new StockItem(sku, available);
  }

  public boolean canReserve(int quantity) {
    return quantity > 0 && quantity <= available;
  }

  public StockItem reserve(int quantity) {
    if (!canReserve(quantity)) {
      throw new InvalidStock(
          "cannot reserve " + quantity + " of sku " + sku + ", available " + available);
    }
    return new StockItem(sku, available - quantity);
  }
}
