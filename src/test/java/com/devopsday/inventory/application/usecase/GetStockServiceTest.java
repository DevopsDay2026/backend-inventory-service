package com.devopsday.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devopsday.inventory.domain.exception.StockNotFound;
import com.devopsday.inventory.domain.model.StockItem;
import com.devopsday.inventory.testsupport.InMemoryStockRepository;
import org.junit.jupiter.api.Test;

class GetStockServiceTest {

  private final InMemoryStockRepository stock = new InMemoryStockRepository().withStock("SKU-1", 4);
  private final GetStockService service = new GetStockService(stock);

  @Test
  void returnsExistingStock() {
    assertThat(service.bySku("SKU-1")).isEqualTo(StockItem.of("SKU-1", 4));
  }

  @Test
  void failsForUnknownSku() {
    assertThatThrownBy(() -> service.bySku("SKU-9"))
        .isInstanceOf(StockNotFound.class)
        .hasMessageContaining("SKU-9");
  }
}
