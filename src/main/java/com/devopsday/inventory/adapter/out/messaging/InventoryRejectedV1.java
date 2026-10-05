package com.devopsday.inventory.adapter.out.messaging;

import com.devopsday.inventory.domain.event.InventoryRejected;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.UUID;

@RegisterForReflection
public record InventoryRejectedV1(UUID eventId, Instant occurredAt, UUID orderId, String reason) {

  public static final String TYPE = "InventoryRejectedV1";

  public static InventoryRejectedV1 from(InventoryRejected event) {
    return new InventoryRejectedV1(
        event.eventId(), event.occurredAt(), event.orderId().value(), event.reason());
  }
}
