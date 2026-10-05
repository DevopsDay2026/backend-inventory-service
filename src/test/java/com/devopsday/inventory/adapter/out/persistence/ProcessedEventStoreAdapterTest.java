package com.devopsday.inventory.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.devopsday.inventory.testsupport.InMemoryMessagingProfile;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(InMemoryMessagingProfile.class)
class ProcessedEventStoreAdapterTest {

  @Inject ProcessedEventStoreAdapter store;

  @Test
  @TestTransaction
  void firstDeliveryIsNewAndRedeliveryIsNot() {
    var eventId = UUID.randomUUID();

    assertThat(store.markIfNew(eventId)).isTrue();
    assertThat(store.markIfNew(eventId)).isFalse();
  }

  @Test
  @TestTransaction
  void differentEventsAreIndependent() {
    assertThat(store.markIfNew(UUID.randomUUID())).isTrue();
    assertThat(store.markIfNew(UUID.randomUUID())).isTrue();
  }
}
