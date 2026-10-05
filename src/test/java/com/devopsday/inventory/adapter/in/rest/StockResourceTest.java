package com.devopsday.inventory.adapter.in.rest;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.devopsday.inventory.application.port.in.GetStockQuery;
import com.devopsday.inventory.application.port.in.SetStockUseCase;
import com.devopsday.inventory.domain.exception.InvalidStock;
import com.devopsday.inventory.domain.exception.StockNotFound;
import com.devopsday.inventory.domain.model.StockItem;
import com.devopsday.inventory.testsupport.InMemoryMessagingProfile;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
@TestProfile(InMemoryMessagingProfile.class)
class StockResourceTest {

  private static final String PROBLEM_JSON = "application/problem+json";

  @InjectMock SetStockUseCase setStock;
  @InjectMock GetStockQuery getStock;

  @Test
  void getReturnsStock() {
    when(getStock.bySku("SKU-1")).thenReturn(StockItem.of("SKU-1", 7));

    given()
        .when()
        .get("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("sku", equalTo("SKU-1"))
        .body("available", equalTo(7));
  }

  @Test
  void unknownSkuReturns404Problem() {
    when(getStock.bySku("SKU-9")).thenThrow(new StockNotFound("SKU-9"));

    given()
        .when()
        .get("/api/v1/stock/{sku}", "SKU-9")
        .then()
        .statusCode(404)
        .contentType(PROBLEM_JSON)
        .body("type", equalTo("urn:problem:stock-not-found"))
        .body("status", equalTo(404))
        .body("detail", containsString("SKU-9"));
  }

  @Test
  void tooLongSkuOnGetReturns400Problem() {
    given()
        .when()
        .get("/api/v1/stock/{sku}", "S".repeat(65))
        .then()
        .statusCode(400)
        .contentType(PROBLEM_JSON)
        .body("type", equalTo("urn:problem:validation"));

    verifyNoInteractions(getStock);
  }

  @Test
  void putSetsStockAndReturnsIt() {
    when(setStock.set(any())).thenReturn(StockItem.of("SKU-1", 10));

    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"available":10}
            """)
        .when()
        .put("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON)
        .body("sku", equalTo("SKU-1"))
        .body("available", equalTo(10));

    var command = ArgumentCaptor.forClass(SetStockUseCase.Command.class);
    verify(setStock).set(command.capture());
    assertThat(command.getValue()).isEqualTo(new SetStockUseCase.Command("SKU-1", 10));
  }

  @Test
  void putAcceptsZeroAndTheMaximum() {
    when(setStock.set(any())).thenReturn(StockItem.of("SKU-1", 0));

    for (var value : new int[] {0, 1_000_000}) {
      given()
          .contentType(ContentType.JSON)
          .body("{\"available\":" + value + "}")
          .when()
          .put("/api/v1/stock/{sku}", "SKU-1")
          .then()
          .statusCode(200);
    }
  }

  @Test
  void negativeAvailableReturnsValidationProblem() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"available":-1}
            """)
        .when()
        .put("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(400)
        .contentType(PROBLEM_JSON)
        .body("type", equalTo("urn:problem:validation"))
        .body("title", equalTo("Validation failed"))
        .body("errors.field", hasItems("available"));

    verifyNoInteractions(setStock);
  }

  @Test
  void availableAboveMaximumReturnsValidationProblem() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"available":1000001}
            """)
        .when()
        .put("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(400)
        .contentType(PROBLEM_JSON)
        .body("errors.field", hasItems("available"));

    verifyNoInteractions(setStock);
  }

  @Test
  void missingAvailableReturnsValidationProblem() {
    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .put("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(400)
        .contentType(PROBLEM_JSON)
        .body("errors.field", hasItems("available"));

    verifyNoInteractions(setStock);
  }

  @Test
  void tooLongSkuOnPutReturnsValidationProblem() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"available\":1}")
        .when()
        .put("/api/v1/stock/{sku}", "S".repeat(65))
        .then()
        .statusCode(400)
        .contentType(PROBLEM_JSON)
        .body("type", equalTo("urn:problem:validation"));

    verifyNoInteractions(setStock);
  }

  @Test
  void malformedJsonReturnsProblemDetails() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"available\":")
        .when()
        .put("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(400)
        .contentType(PROBLEM_JSON)
        .body("status", equalTo(400));

    verifyNoInteractions(setStock);
  }

  @Test
  void wrongJsonTypeReturnsProblemDetailsWithoutInternals() {
    given()
        .contentType(ContentType.JSON)
        .body(
            """
            {"available":[1,2]}
            """)
        .when()
        .put("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(400)
        .contentType(PROBLEM_JSON)
        .body("type", equalTo("urn:problem:malformed-request"))
        .body("detail", not(containsString("com.")));

    verifyNoInteractions(setStock);
  }

  @Test
  void domainRejectionReturns400Problem() {
    when(setStock.set(any())).thenThrow(new InvalidStock("available must not be negative"));

    given()
        .contentType(ContentType.JSON)
        .body("{\"available\":1}")
        .when()
        .put("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(400)
        .contentType(PROBLEM_JSON)
        .body("type", equalTo("urn:problem:invalid-stock"));
  }

  @Test
  void unsupportedMethodReturns405Problem() {
    given()
        .when()
        .delete("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(405)
        .contentType(PROBLEM_JSON)
        .body("type", equalTo("urn:problem:http-405"));
  }

  @Test
  void unexpectedErrorReturns500ProblemWithoutInternals() {
    when(getStock.bySku("SKU-1")).thenThrow(new IllegalStateException("jdbc:postgresql://secret"));

    given()
        .when()
        .get("/api/v1/stock/{sku}", "SKU-1")
        .then()
        .statusCode(500)
        .contentType(PROBLEM_JSON)
        .body("type", equalTo("urn:problem:internal-error"))
        .body("detail", equalTo("An unexpected error occurred"));
  }

  @Test
  void openApiDocumentListsTheStockApi() {
    given()
        .accept(ContentType.JSON)
        .when()
        .get("/q/openapi")
        .then()
        .statusCode(200)
        .body("paths.'/api/v1/stock/{sku}'.put", not(nullValue()));
  }
}
