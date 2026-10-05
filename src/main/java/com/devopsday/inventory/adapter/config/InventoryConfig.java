package com.devopsday.inventory.adapter.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "inventory")
public interface InventoryConfig {

  Topics topics();

  Outbox outbox();

  interface Topics {

    String inventoryReserved();

    String inventoryRejected();
  }

  interface Outbox {

    @WithDefault("1s")
    String pollInterval();

    @WithDefault("100")
    int batchSize();
  }
}
