package com.devopsday.inventory.adapter.in.messaging;

public final class MalformedEventException extends RuntimeException {

  public MalformedEventException(String eventType, Throwable cause) {
    super("malformed " + eventType + " event", cause);
  }
}
