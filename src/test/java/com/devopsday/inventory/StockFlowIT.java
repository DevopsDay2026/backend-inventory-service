package com.devopsday.inventory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class StockFlowIT {

  @Test
  void setsAndReadsBackStockThroughRealAdapters() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"available\":12}")
        .when()
        .put("/api/v1/stock/SKU-IT")
        .then()
        .statusCode(200)
        .body("sku", equalTo("SKU-IT"))
        .body("available", equalTo(12));

    given()
        .when()
        .get("/api/v1/stock/SKU-IT")
        .then()
        .statusCode(200)
        .body("available", equalTo(12));

    given()
        .contentType(ContentType.JSON)
        .body("{\"available\":5}")
        .when()
        .put("/api/v1/stock/SKU-IT")
        .then()
        .statusCode(200)
        .body("available", equalTo(5));
  }

  @Test
  void unknownSkuIsAProblemDetail() {
    given()
        .when()
        .get("/api/v1/stock/SKU-NEVER-LOADED")
        .then()
        .statusCode(404)
        .contentType("application/problem+json");
  }

  @Test
  void validationErrorsAreProblemDetails() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"available\":-3}")
        .when()
        .put("/api/v1/stock/SKU-IT")
        .then()
        .statusCode(400)
        .contentType("application/problem+json");
  }

  @Test
  void readinessCoversDatabaseAndKafka() {
    given().when().get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"));
  }
}
