package com.devopsday.inventory.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devopsday.inventory.application.port.in.ReserveStockUseCase;
import com.devopsday.inventory.application.port.in.ReserveStockUseCase.Command.Line;
import com.devopsday.inventory.domain.exception.InvalidStock;
import com.devopsday.inventory.domain.model.OrderId;
import com.devopsday.inventory.domain.model.ReservationResult;
import com.devopsday.inventory.testsupport.InMemoryMessagingProfile;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(InMemoryMessagingProfile.class)
class OrderPlacedConsumerTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  @Inject @Any InMemoryConnector connector;
  @InjectMock ReserveStockUseCase reserveStock;

  @Test
  void orderPlacedIsMappedToAReserveCommand() {
    var eventId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    when(reserveStock.reserve(any())).thenReturn(new ReservationResult.Reserved());

    connector.source("orders-placed").send(payload(eventId, orderId));

    await()
        .atMost(TIMEOUT)
        .untilAsserted(
            () ->
                verify(reserveStock)
                    .reserve(
                        new ReserveStockUseCase.Command(
                            eventId,
                            new OrderId(orderId),
                            List.of(new Line("SKU-1", 2), new Line("SKU-2", 1)))));
  }

  @Test
  void unknownFieldsAreIgnoredForForwardCompatibility() {
    var eventId = UUID.randomUUID();
    var orderId = UUID.randomUUID();

    connector
        .source("orders-placed")
        .send(
            """
            {"eventId":"%s","orderId":"%s","lines":[{"sku":"SKU-1","quantity":1,"gift":true}],"addedLater":1}
            """
                .formatted(eventId, orderId));

    await()
        .atMost(TIMEOUT)
        .untilAsserted(
            () ->
                verify(reserveStock)
                    .reserve(
                        new ReserveStockUseCase.Command(
                            eventId, new OrderId(orderId), List.of(new Line("SKU-1", 1)))));
  }

  @Test
  void rejectedReservationIsAcknowledgedBecauseItIsABusinessOutcome() {
    when(reserveStock.reserve(any())).thenReturn(new ReservationResult.Rejected("unknown sku X"));
    var acked = new AtomicBoolean();

    sendMessage(
        payload(UUID.randomUUID(), UUID.randomUUID()), acked, new AtomicReference<Throwable>());

    await().atMost(TIMEOUT).untilAsserted(() -> assertThat(acked).isTrue());
  }

  @Test
  void malformedPayloadIsNackedSoItGoesToTheDeadLetterQueue() {
    var failure = sendAndCaptureNack("{not-json");

    await().atMost(TIMEOUT).untilAsserted(() -> assertThat(failure.get()).isNotNull());
    assertThat(failure.get()).isInstanceOf(MalformedEventException.class);
    verifyNoInteractions(reserveStock);
  }

  @Test
  void payloadWithoutLinesIsNacked() {
    var failure =
        sendAndCaptureNack(
            "{\"eventId\":\"%s\",\"orderId\":\"%s\"}"
                .formatted(UUID.randomUUID(), UUID.randomUUID()));

    await().atMost(TIMEOUT).untilAsserted(() -> assertThat(failure.get()).isNotNull());
    assertThat(failure.get()).isInstanceOf(MalformedEventException.class);
    verifyNoInteractions(reserveStock);
  }

  @Test
  void jsonNullPayloadIsNacked() {
    var failure = sendAndCaptureNack("null");

    await().atMost(TIMEOUT).untilAsserted(() -> assertThat(failure.get()).isNotNull());
    assertThat(failure.get()).isInstanceOf(MalformedEventException.class);
    verifyNoInteractions(reserveStock);
  }

  @Test
  void useCaseFailureIsNackedWithoutRetry() {
    doThrow(new InvalidStock("quantity must be positive")).when(reserveStock).reserve(any());

    var failure = sendAndCaptureNack(payload(UUID.randomUUID(), UUID.randomUUID()));

    await().atMost(TIMEOUT).untilAsserted(() -> assertThat(failure.get()).isNotNull());
    assertThat(failure.get()).isInstanceOf(InvalidStock.class);
    verify(reserveStock, times(1)).reserve(any());
  }

  @Test
  void optimisticLockConflictIsRetriedUntilItSucceeds() {
    when(reserveStock.reserve(any()))
        .thenThrow(new OptimisticLockException("stale"))
        .thenThrow(new OptimisticLockException("stale"))
        .thenReturn(new ReservationResult.Reserved());
    var acked = new AtomicBoolean();
    var failure = new AtomicReference<Throwable>();

    sendMessage(payload(UUID.randomUUID(), UUID.randomUUID()), acked, failure);

    await().atMost(TIMEOUT).untilAsserted(() -> assertThat(acked).isTrue());
    verify(reserveStock, times(3)).reserve(any());
    assertThat(failure.get()).isNull();
  }

  @Test
  void persistentOptimisticLockConflictIsNackedAfterRetriesAreExhausted() {
    doThrow(new OptimisticLockException("stale")).when(reserveStock).reserve(any());

    var failure = sendAndCaptureNack(payload(UUID.randomUUID(), UUID.randomUUID()));

    await().atMost(TIMEOUT).untilAsserted(() -> assertThat(failure.get()).isNotNull());
    assertThat(failure.get()).isInstanceOf(OptimisticLockException.class);
    verify(reserveStock, times(4)).reserve(any());
  }

  private static String payload(UUID eventId, UUID orderId) {
    return """
        {"eventId":"%s","occurredAt":"2026-01-01T10:00:00Z","orderId":"%s","customerId":"customer-1","lines":[{"sku":"SKU-1","quantity":2},{"sku":"SKU-2","quantity":1}]}
        """
        .formatted(eventId, orderId);
  }

  private AtomicReference<Throwable> sendAndCaptureNack(String payload) {
    var failure = new AtomicReference<Throwable>();
    sendMessage(payload, new AtomicBoolean(), failure);
    return failure;
  }

  private void sendMessage(String payload, AtomicBoolean acked, AtomicReference<Throwable> nack) {
    connector
        .<Message<String>>source("orders-placed")
        .send(
            Message.of(
                payload,
                () -> {
                  acked.set(true);
                  return CompletableFuture.completedFuture(null);
                },
                reason -> {
                  nack.set(reason);
                  return CompletableFuture.completedFuture(null);
                }));
  }
}
