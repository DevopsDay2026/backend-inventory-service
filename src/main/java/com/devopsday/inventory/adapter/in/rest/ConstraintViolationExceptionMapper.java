package com.devopsday.inventory.adapter.in.rest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Provider
public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<ConstraintViolationException> {

  private static final int METHOD_AND_PARAMETER_NODES = 2;

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    var errors =
        exception.getConstraintViolations().stream()
            .map(ConstraintViolationExceptionMapper::toFieldError)
            .sorted(Comparator.comparing(Problem.FieldError::field))
            .toList();
    var status = Response.Status.BAD_REQUEST.getStatusCode();
    return new Problem(
            "urn:problem:validation",
            "Validation failed",
            status,
            "The request has invalid fields",
            errors)
        .toResponse();
  }

  private static Problem.FieldError toFieldError(ConstraintViolation<?> violation) {
    return new Problem.FieldError(fieldOf(violation.getPropertyPath()), violation.getMessage());
  }

  private static String fieldOf(Path path) {
    var field =
        StreamSupport.stream(path.spliterator(), false)
            .skip(METHOD_AND_PARAMETER_NODES)
            .map(Path.Node::toString)
            .collect(Collectors.joining("."));
    return field.isEmpty() ? "body" : field;
  }
}
