package com.devopsday.inventory.domain.exception;

public final class InvalidStock extends DomainException {

  public InvalidStock(String message) {
    super("invalid-stock", message);
  }
}
