package com.devopsday.inventory.adapter.out.messaging;

import com.devopsday.inventory.domain.event.InventoryReserved;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.UUID;

@RegisterForReflection
public record InventoryReservedV1(UUID eventId, Instant occurredAt, UUID orderId) {

  public static final String TYPE = "InventoryReservedV1";

  public static InventoryReservedV1 from(InventoryReserved event) {
    return new InventoryReservedV1(event.eventId(), event.occurredAt(), event.orderId().value());
  }
}
