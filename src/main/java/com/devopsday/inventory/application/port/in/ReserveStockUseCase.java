package com.devopsday.inventory.application.port.in;

import com.devopsday.inventory.domain.model.OrderId;
import com.devopsday.inventory.domain.model.ReservationResult;
import java.util.List;
import java.util.UUID;

public interface ReserveStockUseCase {

  ReservationResult reserve(Command command);

  record Command(UUID eventId, OrderId orderId, List<Line> lines) {

    public Command {
      lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public record Line(String sku, int quantity) {}
  }
}
