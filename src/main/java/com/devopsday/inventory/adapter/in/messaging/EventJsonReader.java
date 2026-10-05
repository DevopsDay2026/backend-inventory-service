package com.devopsday.inventory.adapter.in.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventJsonReader {

  private final ObjectMapper json;

  EventJsonReader(ObjectMapper json) {
    this.json = json;
  }

  public <T> T read(String payload, Class<T> type) {
    try {
      var event = json.readValue(payload, type);
      if (event == null) {
        throw new MalformedEventException(type.getSimpleName(), null);
      }
      return event;
    } catch (JsonProcessingException e) {
      throw new MalformedEventException(type.getSimpleName(), e);
    }
  }
}
