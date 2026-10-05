package com.devopsday.inventory.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.ws.rs.core.Response;
import java.util.List;

/** RFC 9457 Problem Details. */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Problem(
    String type, String title, int status, String detail, List<FieldError> errors) {

  public static final String MEDIA_TYPE = "application/problem+json";

  @RegisterForReflection
  public record FieldError(String field, String message) {}

  public Problem {
    errors = errors == null ? List.of() : List.copyOf(errors);
  }

  public static Problem of(String code, String title, int status, String detail) {
    return new Problem("urn:problem:" + code, title, status, detail, List.of());
  }

  public Response toResponse() {
    return Response.status(status).type(MEDIA_TYPE).entity(this).build();
  }
}
