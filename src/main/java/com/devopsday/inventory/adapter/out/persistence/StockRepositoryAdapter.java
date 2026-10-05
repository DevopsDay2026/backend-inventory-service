package com.devopsday.inventory.adapter.out.persistence;

import com.devopsday.inventory.application.port.out.StockRepository;
import com.devopsday.inventory.domain.model.StockItem;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class StockRepositoryAdapter implements StockRepository {

  private final StockPanacheRepository repository;
  private final StockEntityMapper mapper;

  StockRepositoryAdapter(StockPanacheRepository repository, StockEntityMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Optional<StockItem> findBySku(String sku) {
    return repository.findBySku(sku).map(mapper::toDomain);
  }

  @Override
  public void save(StockItem item) {
    repository
        .findBySku(item.sku())
        .ifPresentOrElse(
            entity -> {
              mapper.applyState(item, entity);
              repository.flush();
            },
            () -> repository.persist(mapper.toNewEntity(item)));
  }
}
