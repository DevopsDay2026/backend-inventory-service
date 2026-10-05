package com.devopsday.inventory.adapter.out.messaging;

import com.devopsday.inventory.adapter.config.InventoryConfig;
import com.devopsday.inventory.adapter.out.persistence.OutboxEntity;
import com.devopsday.inventory.adapter.out.persistence.OutboxRepository;
import com.devopsday.inventory.application.port.out.InventoryEventPublisher;
import com.devopsday.inventory.domain.event.DomainEvent;
import com.devopsday.inventory.domain.event.InventoryRejected;
import com.devopsday.inventory.domain.event.InventoryReserved;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OutboxInventoryEventPublisher implements InventoryEventPublisher {

  private final OutboxRepository outbox;
  private final ObjectMapper json;
  private final InventoryConfig config;

  OutboxInventoryEventPublisher(
      OutboxRepository outbox, ObjectMapper json, InventoryConfig config) {
    this.outbox = outbox;
    this.json = json;
    this.config = config;
  }

  @Override
  public void publish(DomainEvent event) {
    var row =
        switch (event) {
          case InventoryReserved reserved ->
              OutboxEntity.pending(
                  reserved.eventId(),
                  reserved.aggregateId(),
                  InventoryReservedV1.TYPE,
                  config.topics().inventoryReserved(),
                  toJson(InventoryReservedV1.from(reserved)),
                  reserved.occurredAt());
          case InventoryRejected rejected ->
              OutboxEntity.pending(
                  rejected.eventId(),
                  rejected.aggregateId(),
                  InventoryRejectedV1.TYPE,
                  config.topics().inventoryRejected(),
                  toJson(InventoryRejectedV1.from(rejected)),
                  rejected.occurredAt());
        };
    outbox.persist(row);
  }

  private String toJson(Object payload) {
    try {
      return json.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("cannot serialize " + payload.getClass().getSimpleName(), e);
    }
  }
}
