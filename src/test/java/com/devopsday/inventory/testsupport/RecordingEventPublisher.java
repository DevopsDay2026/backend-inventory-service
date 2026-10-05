package com.devopsday.inventory.testsupport;

import com.devopsday.inventory.application.port.out.InventoryEventPublisher;
import com.devopsday.inventory.domain.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;

public final class RecordingEventPublisher implements InventoryEventPublisher {

  private final List<DomainEvent> published = new ArrayList<>();

  @Override
  public void publish(DomainEvent event) {
    published.add(event);
  }

  public List<DomainEvent> published() {
    return List.copyOf(published);
  }
}
