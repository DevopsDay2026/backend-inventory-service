package com.devopsday.inventory.testsupport;

import com.devopsday.inventory.application.port.out.ProcessedEventStore;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class InMemoryProcessedEventStore implements ProcessedEventStore {

  private final Set<UUID> seen = new HashSet<>();

  @Override
  public boolean markIfNew(UUID eventId) {
    return seen.add(eventId);
  }
}
