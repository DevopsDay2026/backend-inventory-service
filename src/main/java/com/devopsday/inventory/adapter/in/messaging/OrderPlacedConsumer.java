package com.devopsday.inventory.adapter.in.messaging;

import com.devopsday.inventory.application.port.in.ReserveStockUseCase;
import com.devopsday.inventory.domain.model.OrderId;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class OrderPlacedConsumer {

  private final ReserveStockUseCase reserveStock;
  private final EventJsonReader reader;

  OrderPlacedConsumer(ReserveStockUseCase reserveStock, EventJsonReader reader) {
    this.reserveStock = reserveStock;
    this.reader = reader;
  }

  @Incoming("orders-placed")
  @Blocking
  @Retry(maxRetries = 3, delay = 200, retryOn = PersistenceException.class)
  public void on(String payload) {
    var event = reader.read(payload, OrderPlacedV1.class);
    var lines =
        event.lines().stream()
            .map(line -> new ReserveStockUseCase.Command.Line(line.sku(), line.quantity()))
            .toList();
    reserveStock.reserve(
        new ReserveStockUseCase.Command(event.eventId(), new OrderId(event.orderId()), lines));
  }
}
