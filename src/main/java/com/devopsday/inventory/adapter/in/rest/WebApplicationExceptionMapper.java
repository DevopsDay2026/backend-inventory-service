package com.devopsday.inventory.adapter.in.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

  @Override
  public Response toResponse(WebApplicationException exception) {
    var status = exception.getResponse().getStatusInfo();
    return Problem.of(
            "http-" + status.getStatusCode(),
            status.getReasonPhrase(),
            status.getStatusCode(),
            null)
        .toResponse();
  }
}
