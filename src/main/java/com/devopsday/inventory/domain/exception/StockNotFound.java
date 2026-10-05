package com.devopsday.inventory.domain.exception;

public final class StockNotFound extends DomainException {

  public StockNotFound(String sku) {
    super("stock-not-found", "no stock registered for sku " + sku);
  }
}
