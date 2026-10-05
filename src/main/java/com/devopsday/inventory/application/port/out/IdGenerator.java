package com.devopsday.inventory.application.port.out;

import java.util.UUID;

public interface IdGenerator {

  UUID newId();
}
