package com.devopsday.inventory.adapter.config;

import com.devopsday.inventory.application.port.out.Clock;
import com.devopsday.inventory.application.port.out.IdGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class BeanConfig {

  @Produces
  @ApplicationScoped
  Clock clock() {
    return Instant::now;
  }

  @Produces
  @ApplicationScoped
  IdGenerator idGenerator() {
    return UUID::randomUUID;
  }
}
