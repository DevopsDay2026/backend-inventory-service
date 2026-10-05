# ADR-0005: Un esquema de PostgreSQL por servicio

- Estado: Aceptado
- Fecha: 2026-10-04

## Contexto

En la laptop `orders-service` e `inventory-service` usan el mismo PostgreSQL y la misma base
(`jdbc:postgresql://localhost:5432/postgres`). Los dos crean las tablas `outbox`, `processed_event`
y `flyway_schema_history`: en el esquema `public` el segundo servicio en arrancar falla o, peor,
comparte el outbox del primero. En OpenShift y ROSA cada servicio tiene su propia base.

## Decisión

- Cada servicio es dueño de un esquema con su nombre (`inventory` aquí, `orders` en el otro servicio),
  configurable con `DB_SCHEMA`.
- `quarkus.flyway.schemas` crea el esquema y guarda ahí el historial de migraciones.
- `currentSchema` en las propiedades JDBC fija el `search_path` de todas las conexiones, de modo que
  Hibernate, las consultas nativas y las migraciones usan nombres de tabla sin calificar.

## Consecuencias

- La misma configuración sirve con una base compartida (laptop) y con una base por servicio
  (clúster): no cambia ningún `DB_URL` ni ningún manifiesto.
- El usuario de la base necesita permiso para crear esquemas la primera vez (lo tiene el dueño de la
  base o un superusuario).
- Las tablas que versiones anteriores hayan creado en `public` quedan huérfanas y se pueden borrar.
