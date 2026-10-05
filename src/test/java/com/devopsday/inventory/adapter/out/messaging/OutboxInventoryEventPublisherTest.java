package com.devopsday.inventory.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.devopsday.inventory.adapter.out.persistence.OutboxRepository;
import com.devopsday.inventory.domain.event.InventoryRejected;
import com.devopsday.inventory.domain.event.InventoryReserved;
import com.devopsday.inventory.domain.model.OrderId;
import com.devopsday.inventory.testsupport.InMemoryMessagingProfile;
import com.devopsday.inventory.testsupport.StockFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(InMemoryMessagingProfile.class)
class OutboxInventoryEventPublisherTest {

  @Inject OutboxInventoryEventPublisher publisher;
  @Inject OutboxRepository outbox;
  @Inject ObjectMapper json;

  @Test
  @TestTransaction
  void storesPendingReservedRowKeyedByOrderId() throws Exception {
    var orderId = new OrderId(UUID.randomUUID());
    var event = new InventoryReserved(UUID.randomUUID(), StockFixtures.NOW, orderId);

    publisher.publish(event);
    outbox.flush();
    outbox.getEntityManager().clear();

    var row = outbox.findById(event.eventId());
    assertThat(row.eventType()).isEqualTo("InventoryReservedV1");
    assertThat(row.topic()).isEqualTo("inventory.reserved");
    assertThat(row.messageKey()).isEqualTo(orderId.toString());
    assertThat(row.sentAt()).isNull();

    var payload = json.readTree(row.payload());
    assertThat(payload.get("eventId").asText()).isEqualTo(event.eventId().toString());
    assertThat(payload.get("occurredAt").asText()).isEqualTo("2026-01-01T10:00:00Z");
    assertThat(payload.get("orderId").asText()).isEqualTo(orderId.toString());
    assertThat(payload.has("reason")).isFalse();
  }

  @Test
  @TestTransaction
  void storesPendingRejectedRowWithReason() throws Exception {
    var orderId = new OrderId(UUID.randomUUID());
    var event =
        new InventoryRejected(
            UUID.randomUUID(), StockFixtures.NOW, orderId, "insufficient stock for SKU-1");

    publisher.publish(event);
    outbox.flush();
    outbox.getEntityManager().clear();

    var row = outbox.findById(event.eventId());
    assertThat(row.eventType()).isEqualTo("InventoryRejectedV1");
    assertThat(row.topic()).isEqualTo("inventory.rejected");
    assertThat(row.messageKey()).isEqualTo(orderId.toString());

    var payload = json.readTree(row.payload());
    assertThat(payload.get("orderId").asText()).isEqualTo(orderId.toString());
    assertThat(payload.get("reason").asText()).isEqualTo("insufficient stock for SKU-1");
    assertThat(payload.get("occurredAt").asText()).isEqualTo("2026-01-01T10:00:00Z");
  }
}
