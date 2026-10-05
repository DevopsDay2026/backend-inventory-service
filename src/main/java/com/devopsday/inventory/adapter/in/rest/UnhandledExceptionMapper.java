package com.devopsday.inventory.adapter.in.rest;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable exception) {
    Log.error("unhandled error while processing a request", exception);
    var status = Response.Status.INTERNAL_SERVER_ERROR;
    return Problem.of(
            "internal-error",
            status.getReasonPhrase(),
            status.getStatusCode(),
            "An unexpected error occurred")
        .toResponse();
  }
}
