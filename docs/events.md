# Contrato de eventos

Este documento es el contrato entre `backend-orders-service` y `backend-inventory-service`.
Se mantiene **idéntico en los dos repositorios**; no existe una librería compartida (ADR-0002).

## Reglas

- El payload es JSON UTF-8. Cada tipo de evento es un record versionado (`...V1`).
- Todo evento lleva `eventId` (UUID, único por evento) y `occurredAt` (ISO-8601 UTC).
- La clave Kafka es el id del agregado (`orderId`), para conservar el orden por pedido.
- Cabeceras: `event-type` (p. ej. `OrderPlacedV1`) y `event-id`. Son informativas: la
  idempotencia usa el `eventId` del payload.
- Entrega *at-least-once*: los consumidores deduplican por `eventId` (tabla `processed_event`).
- Compatibilidad: dentro de una versión solo se agregan campos opcionales; los consumidores ignoran
  campos desconocidos. Un cambio incompatible crea `...V2` en un tópico nuevo.
- Un mensaje que no se puede procesar va al tópico `<tópico>.dlq`.

## Tópicos

| Tópico (por defecto) | Variable | Evento | Productor | Consumidor |
|---|---|---|---|---|
| `orders.placed` | `ORDERS_PLACED_TOPIC` | `OrderPlacedV1` | orders | inventory |
| `inventory.reserved` | `INVENTORY_RESERVED_TOPIC` | `InventoryReservedV1` | inventory | orders |
| `inventory.rejected` | `INVENTORY_REJECTED_TOPIC` | `InventoryRejectedV1` | inventory | orders |

## OrderPlacedV1

```json
{
  "eventId": "6f1c1f0e-6f43-4c0e-9d3b-0b1f0a2f8a11",
  "occurredAt": "2026-01-01T10:00:00Z",
  "orderId": "b3c0a4f2-6a57-4d3a-8a34-0f1f5f9d2c10",
  "customerId": "customer-1",
  "lines": [{ "sku": "SKU-1", "quantity": 2 }]
}
```

## InventoryReservedV1

Todas las líneas del pedido quedaron reservadas.

```json
{
  "eventId": "0c0e1d7a-58f4-4b53-9a51-1d2b8a1c7e21",
  "occurredAt": "2026-01-01T10:00:01Z",
  "orderId": "b3c0a4f2-6a57-4d3a-8a34-0f1f5f9d2c10"
}
```

## InventoryRejectedV1

No se reservó nada (la reserva es todo o nada).

```json
{
  "eventId": "5a7f9f43-2f5e-4c0c-8b56-6d9a4f1f3b77",
  "occurredAt": "2026-01-01T10:00:01Z",
  "orderId": "b3c0a4f2-6a57-4d3a-8a34-0f1f5f9d2c10",
  "reason": "insufficient stock for SKU-1"
}
```
