package com.devopsday.inventory.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "stock")
public class StockEntity {

  @Id UUID id;

  @Column(nullable = false, unique = true)
  String sku;

  @Column(nullable = false)
  int available;

  @Version long version;
}
