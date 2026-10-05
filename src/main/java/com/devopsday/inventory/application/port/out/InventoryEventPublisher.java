package com.devopsday.inventory.application.port.out;

import com.devopsday.inventory.domain.event.DomainEvent;

public interface InventoryEventPublisher {

  void publish(DomainEvent event);
}
