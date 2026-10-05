package com.devopsday.inventory.testsupport;

import com.devopsday.inventory.application.port.out.IdGenerator;
import java.util.UUID;

public final class SequentialIdGenerator implements IdGenerator {

  private long next = 1;

  @Override
  public UUID newId() {
    return new UUID(0, next++);
  }
}
