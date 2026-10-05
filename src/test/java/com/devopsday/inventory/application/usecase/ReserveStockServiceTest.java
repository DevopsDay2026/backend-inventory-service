package com.devopsday.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devopsday.inventory.application.port.in.ReserveStockUseCase.Command;
import com.devopsday.inventory.application.port.in.ReserveStockUseCase.Command.Line;
import com.devopsday.inventory.domain.event.InventoryRejected;
import com.devopsday.inventory.domain.event.InventoryReserved;
import com.devopsday.inventory.domain.exception.InvalidStock;
import com.devopsday.inventory.domain.model.OrderId;
import com.devopsday.inventory.domain.model.ReservationResult;
import com.devopsday.inventory.domain.model.ReservationResult.AlreadyProcessed;
import com.devopsday.inventory.domain.model.ReservationResult.Rejected;
import com.devopsday.inventory.domain.model.ReservationResult.Reserved;
import com.devopsday.inventory.testsupport.InMemoryProcessedEventStore;
import com.devopsday.inventory.testsupport.InMemoryStockRepository;
import com.devopsday.inventory.testsupport.RecordingEventPublisher;
import com.devopsday.inventory.testsupport.SequentialIdGenerator;
import com.devopsday.inventory.testsupport.StockFixtures;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReserveStockServiceTest {

  private final InMemoryStockRepository stock =
      new InMemoryStockRepository().withStock("SKU-1", 5).withStock("SKU-2", 1);
  private final RecordingEventPublisher events = new RecordingEventPublisher();
  private final ReserveStockService service =
      new ReserveStockService(
          stock,
          events,
          new InMemoryProcessedEventStore(),
          () -> StockFixtures.NOW,
          new SequentialIdGenerator());
  private final OrderId orderId = new OrderId(UUID.randomUUID());

  @Test
  void reservesEveryLineAndPublishesInventoryReserved() {
    var result = service.reserve(command(new Line("SKU-1", 2), new Line("SKU-2", 1)));

    assertThat(result).isInstanceOf(Reserved.class);
    assertThat(stock.availableOf("SKU-1")).isEqualTo(3);
    assertThat(stock.availableOf("SKU-2")).isZero();
    assertThat(events.published())
        .singleElement()
        .isInstanceOfSatisfying(
            InventoryReserved.class,
            event -> {
              assertThat(event.orderId()).isEqualTo(orderId);
              assertThat(event.aggregateId()).isEqualTo(orderId.value());
              assertThat(event.eventId()).isNotEqualTo(orderId.value());
              assertThat(event.occurredAt()).isEqualTo(StockFixtures.NOW);
            });
  }

  @Test
  void reservesExactlyTheAvailableQuantity() {
    var result = service.reserve(command(new Line("SKU-2", 1)));

    assertThat(result).isInstanceOf(Reserved.class);
    assertThat(stock.availableOf("SKU-2")).isZero();
  }

  @Test
  void rejectsWhenStockIsInsufficientAndChangesNothing() {
    var result = service.reserve(command(new Line("SKU-1", 6)));

    assertThat(result).isEqualTo(new Rejected("insufficient stock for SKU-1"));
    assertThat(stock.availableOf("SKU-1")).isEqualTo(5);
    assertThat(stock.saveCount()).isZero();
    assertRejectedEventWith("insufficient stock for SKU-1");
  }

  @Test
  void rejectsUnknownSkuWithReason() {
    var result = service.reserve(command(new Line("SKU-9", 1)));

    assertThat(result).isEqualTo(new Rejected("unknown sku SKU-9"));
    assertRejectedEventWith("unknown sku SKU-9");
  }

  @Test
  void aggregatesRepeatedSkusBeforeChecking() {
    var result = service.reserve(command(new Line("SKU-1", 3), new Line("SKU-1", 3)));

    assertThat(result).isEqualTo(new Rejected("insufficient stock for SKU-1"));
    assertThat(stock.availableOf("SKU-1")).isEqualTo(5);
  }

  @Test
  void aggregatesRepeatedSkusIntoASingleDecrement() {
    var result = service.reserve(command(new Line("SKU-1", 2), new Line("SKU-1", 3)));

    assertThat(result).isInstanceOf(Reserved.class);
    assertThat(stock.availableOf("SKU-1")).isZero();
    assertThat(stock.saveCount()).isEqualTo(1);
  }

  @Test
  void aggregatedQuantityBeyondIntRangeIsInsufficientNotAnOverflow() {
    var result =
        service.reserve(
            command(new Line("SKU-1", Integer.MAX_VALUE), new Line("SKU-1", Integer.MAX_VALUE)));

    assertThat(result).isEqualTo(new Rejected("insufficient stock for SKU-1"));
  }

  @Test
  void savesNothingWhenOneLineOfSeveralFails() {
    var result = service.reserve(command(new Line("SKU-1", 2), new Line("SKU-2", 2)));

    assertThat(result).isInstanceOf(Rejected.class);
    assertThat(stock.saveCount()).isZero();
    assertThat(stock.availableOf("SKU-1")).isEqualTo(5);
    assertThat(stock.availableOf("SKU-2")).isEqualTo(1);
    assertThat(events.published()).singleElement().isInstanceOf(InventoryRejected.class);
  }

  @Test
  void rejectedEventCarriesOrderIdAsAggregateId() {
    service.reserve(command(new Line("SKU-9", 1)));

    assertThat(events.published())
        .singleElement()
        .isInstanceOfSatisfying(
            InventoryRejected.class,
            event -> {
              assertThat(event.orderId()).isEqualTo(orderId);
              assertThat(event.aggregateId()).isEqualTo(orderId.value());
              assertThat(event.occurredAt()).isEqualTo(StockFixtures.NOW);
            });
  }

  @Test
  void ignoresRedeliveredEventWithoutTouchingStockOrPublishing() {
    var command = command(new Line("SKU-1", 2));

    service.reserve(command);
    ReservationResult second = service.reserve(command);

    assertThat(second).isInstanceOf(AlreadyProcessed.class);
    assertThat(stock.availableOf("SKU-1")).isEqualTo(3);
    assertThat(events.published()).hasSize(1);
  }

  @Test
  void invalidLineFailsTheWholeCommand() {
    var command = command(new Line("SKU-1", 0));

    assertThatThrownBy(() -> service.reserve(command)).isInstanceOf(InvalidStock.class);

    assertThat(stock.saveCount()).isZero();
    assertThat(events.published()).isEmpty();
  }

  @Test
  void commandToleratesNullLines() {
    assertThat(new Command(UUID.randomUUID(), orderId, null).lines()).isEmpty();
  }

  @Test
  void emptyCommandReservesNothing() {
    var result = service.reserve(command());

    assertThat(result).isInstanceOf(Reserved.class);
    assertThat(stock.saveCount()).isZero();
  }

  private void assertRejectedEventWith(String reason) {
    assertThat(events.published())
        .singleElement()
        .isInstanceOfSatisfying(
            InventoryRejected.class, event -> assertThat(event.reason()).isEqualTo(reason));
  }

  private Command command(Line... lines) {
    return new Command(UUID.randomUUID(), orderId, List.of(lines));
  }
}
