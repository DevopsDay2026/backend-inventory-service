package com.devopsday.inventory.domain.event;

import com.devopsday.inventory.domain.model.OrderId;
import java.time.Instant;
import java.util.UUID;

public record InventoryReserved(UUID eventId, Instant occurredAt, OrderId orderId)
    implements DomainEvent {

  @Override
  public UUID aggregateId() {
    return orderId.value();
  }
}
