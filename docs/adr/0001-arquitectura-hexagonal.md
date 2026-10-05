# ADR-0001: Arquitectura hexagonal

- Estado: Aceptado
- Fecha: 2026-10-03

## Contexto

El servicio debe poder cambiar de infraestructura (broker, base de datos, plataforma) sin tocar las
reglas de negocio, y esas reglas deben probarse sin arrancar Quarkus.

## Decisión

Paquetes fijos: `domain`, `application.port.in`, `application.port.out`, `application.usecase`,
`adapter.in.rest`, `adapter.in.messaging`, `adapter.out.persistence`, `adapter.out.messaging`,
`adapter.config`.

- `domain` y `application` no importan `jakarta.*`, `io.quarkus.*`, `org.hibernate.*`,
  `org.eclipse.microprofile.*` ni `io.smallrye.*`.
- **Única excepción**: las clases de `application.usecase` llevan
  `jakarta.enterprise.context.ApplicationScoped` y `jakarta.transaction.Transactional`. La
  transacción empieza y termina en el caso de uso; así la escritura del agregado, la fila del outbox
  y la marca de idempotencia son atómicas sin que los adaptadores de entrada sepan de transacciones.
- Los casos de uso dependen solo de puertos, con inyección por constructor (package-private).
- El mapeo DTO ↔ dominio ↔ entidad vive en los adaptadores. Los recursos REST, consumidores y
  repositorios no contienen lógica de negocio.
- La idempotencia de los eventos entrantes es un puerto de salida (`ProcessedEventStore`) que
  invoca el caso de uso, no el consumidor: la marca y el cambio de estado van en una transacción.
- El relay y el publicador del outbox (`adapter.out.messaging`) usan `OutboxRepository`
  (`adapter.out.persistence`); es la única colaboración entre adaptadores y es pura infraestructura.

## Alternativas

- Casos de uso 100 % puros cableados con `@Produces`: obliga a abrir la transacción en cada
  adaptador de entrada. Descartada por repetir el límite transaccional en REST y en Kafka.
- Módulos Maven por capa: innecesario para un microservicio de un solo equipo.

## Consecuencias

- Dominio y casos de uso se prueban con JUnit 5 + AssertJ y fakes en memoria, sin `@QuarkusTest`.
- La frontera se verifica con `grep` en el quality gate, permitiendo solo las dos anotaciones.
