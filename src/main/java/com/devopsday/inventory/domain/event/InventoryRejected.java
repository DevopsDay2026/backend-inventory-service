package com.devopsday.inventory.domain.event;

import com.devopsday.inventory.domain.model.OrderId;
import java.time.Instant;
import java.util.UUID;

public record InventoryRejected(UUID eventId, Instant occurredAt, OrderId orderId, String reason)
    implements DomainEvent {

  @Override
  public UUID aggregateId() {
    return orderId.value();
  }
}
