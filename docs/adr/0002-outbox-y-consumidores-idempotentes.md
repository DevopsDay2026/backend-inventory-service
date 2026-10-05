# ADR-0002: Outbox transaccional y consumidores idempotentes

- Estado: Aceptado
- Fecha: 2026-10-03

## Contexto

Guardar en PostgreSQL y publicar en Kafka son dos sistemas distintos: si se publica directo desde el
caso de uso, un fallo entre ambos pierde el evento o publica algo que nunca se guardó.

## Decisión

- **Publicación**: el caso de uso llama al puerto de publicación, cuyo adaptador inserta una fila
  en la tabla `outbox` dentro de la misma transacción. `OutboxRelay` (`@Scheduled`) lee las
  pendientes con `FOR UPDATE SKIP LOCKED`, las envía por SmallRye Reactive Messaging y las marca
  como enviadas solo cuando el broker confirma. Varias réplicas pueden correr el relay a la vez.
- **Consumo**: `@Incoming` + `@Blocking`. El caso de uso registra el `eventId` en `processed_event`
  (`INSERT ... ON CONFLICT DO NOTHING`) en la misma transacción del cambio; un evento repetido no
  hace nada. Los fallos van a `<tópico>.dlq` (`failure-strategy=dead-letter-queue`).
- **Contrato**: records versionados (`InventoryReservedV1`, `InventoryRejectedV1`) con `eventId`,
  `occurredAt` y clave igual al id del agregado (`orderId`), documentados en `docs/events.md` en
  cada repositorio. No hay librería compartida.
- **Deserialización**: el canal entrega `String` y el adaptador lo convierte con Jackson. Un JSON
  inválido se convierte en una excepción del consumidor y termina en la DLQ, en vez de detener el
  canal como haría un fallo dentro de un `Deserializer` de Kafka.
- **Reserva de stock**: es todo o nada y vive en una sola transacción. Cada `StockEntity` tiene
  `@Version`; si dos pedidos compiten por el mismo SKU, el segundo `flush` lanza
  `OptimisticLockException` dentro de la llamada al caso de uso. El consumidor reintenta los fallos de
  persistencia (`PersistenceException`, que incluye ese conflicto; hasta 3 veces, 200 ms de espera); cualquier otro fallo, o agotar los reintentos, hace
  `nack` y el mensaje va a la DLQ. Cada reintento reevalúa el stock actual en una transacción
  nueva, así que el resultado sigue siendo correcto.

## Dependencias que justifica

`quarkus-messaging-kafka`, `quarkus-scheduler` y, en tests,
`smallrye-reactive-messaging-in-memory` y `awaitility`.

`quarkus-smallrye-fault-tolerance` aporta `@Retry` de MicroProfile Fault Tolerance. Es la forma
estándar de reintentar únicamente conflictos de bloqueo optimista en el consumidor sin escribir un
bucle propio ni reintentar errores que no se arreglan solos (JSON inválido, datos inválidos).

## Consecuencias

- Entrega *at-least-once*: un evento puede publicarse dos veces; la idempotencia lo absorbe.
- Hay un retardo de hasta `OUTBOX_POLL_INTERVAL` entre el commit y la publicación.
- La tabla `outbox` crece; las filas enviadas se pueden purgar con un job externo.
- Un SKU muy disputado puede agotar los reintentos y terminar en la DLQ para revisión manual.
