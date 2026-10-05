package com.devopsday.inventory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devopsday.inventory.application.port.in.SetStockUseCase.Command;
import com.devopsday.inventory.domain.exception.InvalidStock;
import com.devopsday.inventory.domain.model.StockItem;
import com.devopsday.inventory.testsupport.InMemoryStockRepository;
import org.junit.jupiter.api.Test;

class SetStockServiceTest {

  private final InMemoryStockRepository stock = new InMemoryStockRepository();
  private final SetStockService service = new SetStockService(stock);

  @Test
  void createsStockForNewSku() {
    var result = service.set(new Command("SKU-1", 10));

    assertThat(result).isEqualTo(StockItem.of("SKU-1", 10));
    assertThat(stock.findBySku("SKU-1")).contains(StockItem.of("SKU-1", 10));
  }

  @Test
  void replacesAvailableOfExistingSku() {
    stock.withStock("SKU-1", 10);

    service.set(new Command("SKU-1", 3));

    assertThat(stock.availableOf("SKU-1")).isEqualTo(3);
  }

  @Test
  void rejectsNegativeAvailableWithoutSaving() {
    var command = new Command("SKU-1", -1);

    assertThatThrownBy(() -> service.set(command)).isInstanceOf(InvalidStock.class);

    assertThat(stock.saveCount()).isZero();
  }
}
