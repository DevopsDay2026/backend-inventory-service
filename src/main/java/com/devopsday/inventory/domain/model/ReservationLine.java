package com.devopsday.inventory.domain.model;

import com.devopsday.inventory.domain.exception.InvalidStock;

public record ReservationLine(String sku, int quantity) {

  public ReservationLine {
    if (sku == null || sku.isBlank()) {
      throw new InvalidStock("sku is required");
    }
    if (quantity <= 0) {
      throw new InvalidStock("quantity must be positive for sku " + sku);
    }
  }
}
