package com.devopsday.inventory.application.usecase;

import com.devopsday.inventory.application.port.in.ReserveStockUseCase;
import com.devopsday.inventory.application.port.out.Clock;
import com.devopsday.inventory.application.port.out.IdGenerator;
import com.devopsday.inventory.application.port.out.InventoryEventPublisher;
import com.devopsday.inventory.application.port.out.ProcessedEventStore;
import com.devopsday.inventory.application.port.out.StockRepository;
import com.devopsday.inventory.domain.event.InventoryRejected;
import com.devopsday.inventory.domain.event.InventoryReserved;
import com.devopsday.inventory.domain.model.OrderId;
import com.devopsday.inventory.domain.model.ReservationLine;
import com.devopsday.inventory.domain.model.ReservationResult;
import com.devopsday.inventory.domain.model.ReservationResult.AlreadyProcessed;
import com.devopsday.inventory.domain.model.ReservationResult.Rejected;
import com.devopsday.inventory.domain.model.ReservationResult.Reserved;
import com.devopsday.inventory.domain.model.StockItem;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class ReserveStockService implements ReserveStockUseCase {

  private final StockRepository stock;
  private final InventoryEventPublisher events;
  private final ProcessedEventStore processedEvents;
  private final Clock clock;
  private final IdGenerator ids;

  ReserveStockService(
      StockRepository stock,
      InventoryEventPublisher events,
      ProcessedEventStore processedEvents,
      Clock clock,
      IdGenerator ids) {
    this.stock = stock;
    this.events = events;
    this.processedEvents = processedEvents;
    this.clock = clock;
    this.ids = ids;
  }

  @Override
  @Transactional
  public ReservationResult reserve(Command command) {
    if (!processedEvents.markIfNew(command.eventId())) {
      return new AlreadyProcessed();
    }
    var requested = aggregate(command);
    var reserved = new ArrayList<StockItem>();
    for (var entry : requested.entrySet()) {
      var sku = entry.getKey();
      var item = stock.findBySku(sku);
      if (item.isEmpty()) {
        return reject(command.orderId(), "unknown sku " + sku);
      }
      if (!item.get().canReserve(entry.getValue())) {
        return reject(command.orderId(), "insufficient stock for " + sku);
      }
      reserved.add(item.get().reserve(entry.getValue()));
    }
    reserved.forEach(stock::save);
    events.publish(new InventoryReserved(ids.newId(), clock.now(), command.orderId()));
    return new Reserved();
  }

  private Rejected reject(OrderId orderId, String reason) {
    events.publish(new InventoryRejected(ids.newId(), clock.now(), orderId, reason));
    return new Rejected(reason);
  }

  private static Map<String, Integer> aggregate(Command command) {
    var quantities = new LinkedHashMap<String, Integer>();
    for (var line : command.lines()) {
      var valid = new ReservationLine(line.sku(), line.quantity());
      quantities.merge(
          valid.sku(), valid.quantity(), (a, b) -> (int) Math.min((long) a + b, Integer.MAX_VALUE));
    }
    return quantities;
  }
}
