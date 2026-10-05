package com.devopsday.inventory.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.devopsday.inventory.adapter.out.persistence.OutboxEntity;
import com.devopsday.inventory.adapter.out.persistence.OutboxRepository;
import com.devopsday.inventory.testsupport.InMemoryMessagingProfile;
import com.devopsday.inventory.testsupport.StockFixtures;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(InMemoryMessagingProfile.class)
class OutboxRelayTest {

  @Inject OutboxRelay relay;
  @Inject OutboxRepository outbox;
  @Inject @Any InMemoryConnector connector;

  @BeforeEach
  void cleanUp() {
    connector.sink("outbox").clear();
    QuarkusTransaction.requiringNew().run(outbox::deleteAll);
  }

  @Test
  void sendsPendingRowsWithKeyTopicAndHeadersAndMarksThemSent() {
    var eventId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    insertPending(eventId, orderId, "InventoryReservedV1", "inventory.reserved");

    relay.relay();

    var messages = connector.sink("outbox").received();
    assertThat(messages).hasSize(1);
    var message = messages.get(0);
    assertThat(message.getPayload()).isEqualTo("{\"orderId\":\"" + orderId + "\"}");
    var metadata = message.getMetadata(OutgoingKafkaRecordMetadata.class).orElseThrow();
    assertThat(metadata.getKey()).isEqualTo(orderId.toString());
    assertThat(metadata.getTopic()).isEqualTo("inventory.reserved");
    assertThat(headerValue(metadata, OutboxRelay.EVENT_TYPE_HEADER))
        .isEqualTo("InventoryReservedV1");
    assertThat(headerValue(metadata, OutboxRelay.EVENT_ID_HEADER)).isEqualTo(eventId.toString());
    assertThat(sentAtOf(eventId)).isNotNull();
  }

  @Test
  void routesEachRowToItsOwnTopic() {
    insertPending(
        UUID.randomUUID(), UUID.randomUUID(), "InventoryReservedV1", "inventory.reserved");
    insertPending(
        UUID.randomUUID(), UUID.randomUUID(), "InventoryRejectedV1", "inventory.rejected");

    relay.relay();

    assertThat(connector.sink("outbox").received())
        .extracting(
            message ->
                message.getMetadata(OutgoingKafkaRecordMetadata.class).orElseThrow().getTopic())
        .containsExactlyInAnyOrder("inventory.reserved", "inventory.rejected");
  }

  @Test
  void doesNotResendRowsAlreadySent() {
    insertPending(
        UUID.randomUUID(), UUID.randomUUID(), "InventoryReservedV1", "inventory.reserved");

    relay.relay();
    relay.relay();

    assertThat(connector.sink("outbox").received()).hasSize(1);
  }

  @Test
  void doesNothingWhenOutboxIsEmpty() {
    relay.relay();

    assertThat(connector.sink("outbox").received()).isEmpty();
  }

  private void insertPending(UUID eventId, UUID orderId, String eventType, String topic) {
    QuarkusTransaction.requiringNew()
        .run(
            () ->
                outbox.persist(
                    OutboxEntity.pending(
                        eventId,
                        orderId,
                        eventType,
                        topic,
                        "{\"orderId\":\"" + orderId + "\"}",
                        StockFixtures.NOW)));
  }

  private Object sentAtOf(UUID eventId) {
    return QuarkusTransaction.requiringNew().call(() -> outbox.findById(eventId).sentAt());
  }

  private static String headerValue(OutgoingKafkaRecordMetadata<?> metadata, String name) {
    return new String(metadata.getHeaders().lastHeader(name).value(), StandardCharsets.UTF_8);
  }
}
