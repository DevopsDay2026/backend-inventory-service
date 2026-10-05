package com.devopsday.inventory.adapter.in.rest;

import com.devopsday.inventory.application.port.in.GetStockQuery;
import com.devopsday.inventory.application.port.in.SetStockUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/stock")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StockResource {

  private final SetStockUseCase setStock;
  private final GetStockQuery getStock;

  StockResource(SetStockUseCase setStock, GetStockQuery getStock) {
    this.setStock = setStock;
    this.getStock = getStock;
  }

  @GET
  @Path("/{sku}")
  public StockResponse get(@PathParam("sku") @NotBlank @Size(max = 64) String sku) {
    return StockResponse.from(getStock.bySku(sku));
  }

  @PUT
  @Path("/{sku}")
  public StockResponse set(
      @PathParam("sku") @NotBlank @Size(max = 64) String sku,
      @NotNull @Valid SetStockRequest request) {
    return StockResponse.from(setStock.set(request.toCommand(sku)));
  }
}
