package com.devopsday.inventory.domain.exception;

public abstract sealed class DomainException extends RuntimeException
    permits StockNotFound, InvalidStock {

  private final String code;

  protected DomainException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
