package com.devopsday.inventory.adapter.in.rest;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class MismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {

  @Override
  public Response toResponse(MismatchedInputException exception) {
    var status = Response.Status.BAD_REQUEST;
    return Problem.of(
            "malformed-request",
            status.getReasonPhrase(),
            status.getStatusCode(),
            "The request body does not match the expected structure")
        .toResponse();
  }
}
