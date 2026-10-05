package com.devopsday.inventory.adapter.in.rest;

import com.devopsday.inventory.domain.exception.DomainException;
import com.devopsday.inventory.domain.exception.InvalidStock;
import com.devopsday.inventory.domain.exception.StockNotFound;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

  @Override
  public Response toResponse(DomainException exception) {
    var status =
        switch (exception) {
          case StockNotFound notFound -> Response.Status.NOT_FOUND;
          case InvalidStock invalid -> Response.Status.BAD_REQUEST;
        };
    return Problem.of(
            exception.code(),
            status.getReasonPhrase(),
            status.getStatusCode(),
            exception.getMessage())
        .toResponse();
  }
}
