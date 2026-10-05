package com.devopsday.inventory.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox")
public class OutboxEntity {

  @Id UUID id;

  @Column(name = "aggregate_id", nullable = false)
  UUID aggregateId;

  @Column(name = "event_type", nullable = false)
  String eventType;

  @Column(nullable = false)
  String topic;

  @Column(name = "message_key", nullable = false)
  String messageKey;

  @Column(nullable = false)
  String payload;

  @Column(name = "created_at", nullable = false)
  Instant createdAt;

  @Column(name = "sent_at")
  Instant sentAt;

  public static OutboxEntity pending(
      UUID eventId,
      UUID aggregateId,
      String eventType,
      String topic,
      String payload,
      Instant createdAt) {
    var entity = new OutboxEntity();
    entity.id = eventId;
    entity.aggregateId = aggregateId;
    entity.eventType = eventType;
    entity.topic = topic;
    entity.messageKey = aggregateId.toString();
    entity.payload = payload;
    entity.createdAt = createdAt;
    return entity;
  }

  public void markSent(Instant when) {
    this.sentAt = when;
  }

  public UUID id() {
    return id;
  }

  public String eventType() {
    return eventType;
  }

  public String topic() {
    return topic;
  }

  public String messageKey() {
    return messageKey;
  }

  public String payload() {
    return payload;
  }

  public Instant sentAt() {
    return sentAt;
  }
}
