package com.devopsday.inventory.domain.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface DomainEvent permits InventoryReserved, InventoryRejected {

  UUID eventId();

  Instant occurredAt();

  UUID aggregateId();
}
