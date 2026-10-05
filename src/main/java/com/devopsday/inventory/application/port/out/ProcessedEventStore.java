package com.devopsday.inventory.application.port.out;

import java.util.UUID;

public interface ProcessedEventStore {

  /** Records the event id; returns {@code false} when it had already been recorded. */
  boolean markIfNew(UUID eventId);
}
