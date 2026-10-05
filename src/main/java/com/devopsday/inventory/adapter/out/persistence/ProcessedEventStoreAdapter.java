package com.devopsday.inventory.adapter.out.persistence;

import com.devopsday.inventory.application.port.out.Clock;
import com.devopsday.inventory.application.port.out.ProcessedEventStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class ProcessedEventStoreAdapter implements ProcessedEventStore {

  private static final String INSERT_IF_ABSENT =
      """
      insert into processed_event (event_id, processed_at)
      values (?1, ?2)
      on conflict (event_id) do nothing
      """;

  private final EntityManager entityManager;
  private final Clock clock;

  ProcessedEventStoreAdapter(EntityManager entityManager, Clock clock) {
    this.entityManager = entityManager;
    this.clock = clock;
  }

  @Override
  public boolean markIfNew(UUID eventId) {
    var inserted =
        entityManager
            .createNativeQuery(INSERT_IF_ABSENT)
            .setParameter(1, eventId)
            .setParameter(2, OffsetDateTime.ofInstant(clock.now(), ZoneOffset.UTC))
            .executeUpdate();
    return inserted == 1;
  }
}
