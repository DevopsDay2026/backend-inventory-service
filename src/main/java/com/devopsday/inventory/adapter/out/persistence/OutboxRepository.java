package com.devopsday.inventory.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OutboxRepository implements PanacheRepositoryBase<OutboxEntity, UUID> {

  private static final String NEXT_BATCH =
      """
      select * from outbox
      where sent_at is null
      order by created_at
      limit ?1
      for update skip locked
      """;

  /** Locks the returned rows so concurrent relays (several replicas) never pick the same batch. */
  public List<OutboxEntity> nextBatch(int size) {
    return getSession()
        .createNativeQuery(NEXT_BATCH, OutboxEntity.class)
        .setParameter(1, size)
        .getResultList();
  }
}
