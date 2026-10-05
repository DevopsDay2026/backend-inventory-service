package com.devopsday.inventory.testsupport;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/**
 * Replaces the Kafka channels with in-memory ones and lets the tests drive the outbox relay. The
 * default configuration keeps the Kafka connector so that {@code @QuarkusIntegrationTest} gets a
 * real broker from Dev Services.
 */
public class InMemoryMessagingProfile implements QuarkusTestProfile {

  private static final String IN_MEMORY = "smallrye-in-memory";

  @Override
  public Map<String, String> getConfigOverrides() {
    return Map.of(
        "mp.messaging.outgoing.outbox.connector",
        IN_MEMORY,
        "mp.messaging.incoming.orders-placed.connector",
        IN_MEMORY,
        "quarkus.kafka.devservices.enabled",
        "false",
        "quarkus.scheduler.enabled",
        "false");
  }
}
