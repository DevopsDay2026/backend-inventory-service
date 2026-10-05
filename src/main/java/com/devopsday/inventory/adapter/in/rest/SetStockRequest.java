package com.devopsday.inventory.adapter.in.rest;

import com.devopsday.inventory.application.port.in.SetStockUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record SetStockRequest(@NotNull @PositiveOrZero @Max(1_000_000) Integer available) {

  public SetStockUseCase.Command toCommand(String sku) {
    return new SetStockUseCase.Command(sku, available);
  }
}
