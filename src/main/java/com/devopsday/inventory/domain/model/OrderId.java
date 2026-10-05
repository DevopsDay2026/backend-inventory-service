package com.devopsday.inventory.domain.model;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {

  public OrderId {
    Objects.requireNonNull(value, "order id is required");
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
