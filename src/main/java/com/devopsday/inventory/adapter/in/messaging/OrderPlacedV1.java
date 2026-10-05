package com.devopsday.inventory.adapter.in.messaging;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RegisterForReflection
public record OrderPlacedV1(
    UUID eventId, Instant occurredAt, UUID orderId, String customerId, List<Line> lines) {

  public OrderPlacedV1 {
    Objects.requireNonNull(eventId, "eventId is required");
    Objects.requireNonNull(orderId, "orderId is required");
    Objects.requireNonNull(lines, "lines is required");
    lines = List.copyOf(lines);
  }

  @RegisterForReflection
  public record Line(String sku, int quantity) {}
}
