package com.devopsday.inventory.adapter.out.messaging;

import com.devopsday.inventory.adapter.config.InventoryConfig;
import com.devopsday.inventory.adapter.out.persistence.OutboxEntity;
import com.devopsday.inventory.adapter.out.persistence.OutboxRepository;
import com.devopsday.inventory.application.port.out.Clock;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;

@ApplicationScoped
public class OutboxRelay {

  static final String EVENT_TYPE_HEADER = "event-type";
  static final String EVENT_ID_HEADER = "event-id";
  private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

  private final OutboxRepository outbox;
  private final MutinyEmitter<String> emitter;
  private final InventoryConfig config;
  private final Clock clock;

  OutboxRelay(
      OutboxRepository outbox,
      @Channel("outbox") MutinyEmitter<String> emitter,
      InventoryConfig config,
      Clock clock) {
    this.outbox = outbox;
    this.emitter = emitter;
    this.config = config;
    this.clock = clock;
  }

  /**
   * At-least-once: a row is marked as sent only after the broker acknowledged it. A crash between
   * the acknowledgement and the commit resends the row, which idempotent consumers absorb.
   */
  @Scheduled(
      every = "{inventory.outbox.poll-interval}",
      delayed = "5s",
      concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
  @Transactional
  void relay() {
    var sent = 0;
    for (var row : outbox.nextBatch(config.outbox().batchSize())) {
      try {
        emitter.sendMessage(toMessage(row)).await().atMost(SEND_TIMEOUT);
      } catch (RuntimeException e) {
        Log.errorf(e, "outbox relay could not send %s %s; will retry", row.eventType(), row.id());
        break;
      }
      row.markSent(clock.now());
      sent++;
    }
    if (sent > 0) {
      Log.debugf("outbox relay sent %d event(s)", sent);
    }
  }

  private static Message<String> toMessage(OutboxEntity row) {
    var headers =
        new RecordHeaders()
            .add(EVENT_TYPE_HEADER, row.eventType().getBytes(StandardCharsets.UTF_8))
            .add(EVENT_ID_HEADER, row.id().toString().getBytes(StandardCharsets.UTF_8));
    var metadata =
        OutgoingKafkaRecordMetadata.<String>builder()
            .withTopic(row.topic())
            .withKey(row.messageKey())
            .withHeaders(headers)
            .build();
    return Message.of(row.payload()).addMetadata(metadata);
  }
}
