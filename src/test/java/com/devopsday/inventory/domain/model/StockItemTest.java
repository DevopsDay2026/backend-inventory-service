package com.devopsday.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devopsday.inventory.domain.exception.InvalidStock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StockItemTest {

  @Test
  void keepsSkuAndAvailable() {
    var item = StockItem.of("SKU-1", 5);

    assertThat(item.sku()).isEqualTo("SKU-1");
    assertThat(item.available()).isEqualTo(5);
  }

  @Test
  void acceptsZeroAvailable() {
    assertThat(StockItem.of("SKU-1", 0).available()).isZero();
  }

  @Test
  void rejectsNegativeAvailable() {
    assertThatThrownBy(() -> StockItem.of("SKU-1", -1))
        .isInstanceOf(InvalidStock.class)
        .hasMessageContaining("available");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void rejectsBlankSku(String sku) {
    assertThatThrownBy(() -> StockItem.of(sku, 1))
        .isInstanceOf(InvalidStock.class)
        .hasMessageContaining("sku");
  }

  @Test
  void canReserveUpToAvailable() {
    var item = StockItem.of("SKU-1", 3);

    assertThat(item.canReserve(1)).isTrue();
    assertThat(item.canReserve(3)).isTrue();
    assertThat(item.canReserve(4)).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void cannotReserveNonPositiveQuantity(int quantity) {
    assertThat(StockItem.of("SKU-1", 3).canReserve(quantity)).isFalse();
  }

  @Test
  void reserveReturnsNewItemAndLeavesOriginalUntouched() {
    var item = StockItem.of("SKU-1", 5);

    var reserved = item.reserve(2);

    assertThat(reserved).isEqualTo(StockItem.of("SKU-1", 3));
    assertThat(item.available()).isEqualTo(5);
  }

  @Test
  void reserveAllLeavesZero() {
    assertThat(StockItem.of("SKU-1", 2).reserve(2).available()).isZero();
  }

  @Test
  void reserveMoreThanAvailableFails() {
    var item = StockItem.of("SKU-1", 1);

    assertThatThrownBy(() -> item.reserve(2))
        .isInstanceOf(InvalidStock.class)
        .hasMessageContaining("SKU-1");
  }

  @Test
  void invalidStockCarriesCode() {
    assertThat(new InvalidStock("x").code()).isEqualTo("invalid-stock");
  }
}
