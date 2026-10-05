package com.devopsday.inventory.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.devopsday.inventory.domain.exception.InvalidStock;
import com.devopsday.inventory.domain.exception.StockNotFound;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ReservationLineTest {

  @Test
  void keepsSkuAndQuantity() {
    var line = new ReservationLine("SKU-1", 3);

    assertThat(line.sku()).isEqualTo("SKU-1");
    assertThat(line.quantity()).isEqualTo(3);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void rejectsNonPositiveQuantity(int quantity) {
    assertThatThrownBy(() -> new ReservationLine("SKU-1", quantity))
        .isInstanceOf(InvalidStock.class)
        .hasMessageContaining("quantity");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void rejectsBlankSku(String sku) {
    assertThatThrownBy(() -> new ReservationLine(sku, 1))
        .isInstanceOf(InvalidStock.class)
        .hasMessageContaining("sku");
  }

  @Test
  void orderIdRequiresValue() {
    assertThatThrownBy(() -> new OrderId(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void orderIdPrintsItsUuid() {
    var uuid = UUID.randomUUID();

    assertThat(new OrderId(uuid)).hasToString(uuid.toString());
  }

  @Test
  void stockNotFoundCarriesCodeAndSku() {
    var exception = new StockNotFound("SKU-9");

    assertThat(exception.code()).isEqualTo("stock-not-found");
    assertThat(exception).hasMessageContaining("SKU-9");
  }
}
