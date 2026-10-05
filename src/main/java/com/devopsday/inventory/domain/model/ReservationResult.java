package com.devopsday.inventory.domain.model;

public sealed interface ReservationResult
    permits ReservationResult.Reserved,
        ReservationResult.Rejected,
        ReservationResult.AlreadyProcessed {

  record Reserved() implements ReservationResult {}

  record Rejected(String reason) implements ReservationResult {}

  record AlreadyProcessed() implements ReservationResult {}
}
