# backend-inventory-service

Servicio de inventario de la charla *"En mi máquina funciona"*. Consume `OrderPlacedV1` de Kafka,
reserva stock en PostgreSQL (todo o nada, con bloqueo optimista `@Version`) y publica
`InventoryReservedV1` o `InventoryRejectedV1` mediante outbox. Su API REST solo consulta y carga
stock. Escucha en el puerto 8081 para convivir con `backend-orders-service` en la misma laptop.

Java 21 · Quarkus 3.33 LTS · Maven · arquitectura hexagonal · imagen nativa compilada dentro de Podman.

## Cómo correrlo en 4 comandos

```bash
cp run.sh.example run.sh     # 1. run.sh está ignorado por git
$EDITOR run.sh               # 2. reemplazar los <...> (usuario y contraseña de PostgreSQL, DOCKER_HOST)
./run.sh infra               # 3. PostgreSQL y Kafka locales, cada uno en su pod de Podman
./run.sh dev                 # 4. modo dev contra localhost:5433 y localhost:9092
```

Requisitos: JDK 21 y Podman (`podman machine start` en macOS y Windows). No se instala GraalVM ni
Docker. En Windows los scripts se ejecutan desde Git Bash.

```bash
curl -i -X PUT localhost:8081/api/v1/stock/SKU-1 -H 'Content-Type: application/json' \
  -d '{"available":10}'
curl localhost:8081/api/v1/stock/SKU-1
```

OpenAPI en `/q/openapi`, Swagger UI (solo dev) en `/q/swagger-ui`, salud en `/q/health`, métricas
en `/q/metrics`.

## Infraestructura local

`./run.sh infra` ejecuta `local-infra.sh`, que crea dos pods independientes con Podman:

| Pod | Imagen | Puerto | Notas |
|---|---|---|---|
| `local-postgres` | `postgres:16-alpine` | 5433 | base `postgres`; usuario y contraseña salen de `DB_USERNAME` y `DB_PASSWORD` de `run.sh` |
| `local-kafka` | `apache/kafka:3.9.0` (KRaft) | 9092 | anuncia `localhost:9092`; crea los tópicos al primer uso |

`./run.sh infra down` los elimina (los datos no se conservan) y `./run.sh infra status` los lista.
Los modos `dev`, `jvm` y `native` usan los mismos valores de `run.sh`.

En Windows los pods viven en la máquina WSL de Podman. PostgreSQL (`localhost:5433`) y Kafka
(`localhost:9092`) se alcanzan igual desde Windows y desde cualquier distribución WSL, así que
`./run.sh dev` y `./run.sh native` usan la misma base y el mismo Kafka. El pod publica 5433 y no
5432 para no chocar con un PostgreSQL instalado en Windows.

Los pods son los mismos para `backend-orders-service` y `backend-inventory-service`: basta con
levantarlos desde uno de los dos repositorios y usar el mismo usuario y contraseña en ambos `run.sh`.
Los dos servicios comparten la base `postgres`; cada uno crea y usa su propio esquema (`orders`,
`inventory`), ver [ADR-0005](docs/adr/0005-esquema-por-servicio.md).

## Los dos backends juntos

```bash
./run.sh infra                                   # una vez, desde cualquiera de los dos repositorios
(cd ../backend-orders-service && ./run.sh dev)       # terminal 1: localhost:8080
(cd ../backend-inventory-service && ./run.sh dev)    # terminal 2: localhost:8081

curl -X PUT localhost:8081/api/v1/stock/SKU-1 -H 'Content-Type: application/json' -d '{"available":5}'
curl -i -X POST localhost:8080/api/v1/orders -H 'Content-Type: application/json' \
  -d '{"customerId":"customer-1","lines":[{"sku":"SKU-1","quantity":2}]}'
curl localhost:8080/api/v1/orders/<id>        # CONFIRMED; con cantidad mayor al stock: REJECTED
curl localhost:8081/api/v1/stock/SKU-1        # available: 3
```

Con el binario nativo es igual, cambiando `./run.sh dev` por `./run.sh native` dentro de WSL.

## Dev Services sobre Podman

Dev Services (solo tests) usa Testcontainers, que busca un socket compatible con Docker.
Hay que apuntarlo al de Podman y desactivar Ryuk:

| Sistema | `DOCKER_HOST` |
|---|---|
| Linux | `unix:///run/user/$UID/podman/podman.sock` (tras `systemctl --user enable --now podman.socket`) |
| macOS | `unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')` |
| Windows | `npipe:////./pipe/podman-machine-default` |

```bash
export DOCKER_HOST=<valor de la tabla>
export TESTCONTAINERS_RYUK_DISABLED=true
```

`run.sh` ya exporta ambas variables; para `./mvnw verify` o `make verify` hay que exportarlas en la
terminal.

## Modos de `run.sh`

| Modo | Qué hace | Necesita |
|---|---|---|
| `./run.sh infra [up\|down\|status]` | pods `local-postgres` y `local-kafka` | Podman |
| `./run.sh dev` | `./mvnw quarkus:dev` | `./run.sh infra` |
| `./run.sh jvm` | `java -jar target/quarkus-app/quarkus-run.jar` | `./mvnw package`, `./run.sh infra` |
| `./run.sh native` | ejecuta `target/*-runner` | `make build-native` (binario Linux), `./run.sh infra` |
| `./run.sh podman` | `podman run` de la imagen `$IMAGE` pasando las variables con `-e NOMBRE` | `make image` |

`dev` usa el perfil `dev`; `jvm`, `native` y `podman` usan `prod`. En todos, si falta una variable
obligatoria la aplicación no arranca. El stack completo en la laptop (PostgreSQL, Kafka y los dos servicios) se
levanta con `deploy-orders-manifests`.

## Variables de entorno

| Variable | Obligatoria | Por defecto | Descripción |
|---|---|---|---|
| `DB_URL` | sí | — | JDBC de PostgreSQL |
| `DB_USERNAME` | sí | — | usuario de la base |
| `DB_PASSWORD` | sí (secreto) | — | contraseña de la base |
| `KAFKA_BOOTSTRAP_SERVERS` | sí | — | brokers Kafka |
| `KAFKA_SECURITY_PROTOCOL` | no | `PLAINTEXT` | p. ej. `SASL_SSL` |
| `KAFKA_SASL_MECHANISM` | no | `PLAIN` | p. ej. `SCRAM-SHA-512` |
| `KAFKA_SASL_JAAS_CONFIG` | no (secreto) | vacío | credenciales SASL |
| `ORDERS_PLACED_TOPIC` | no | `orders.placed` | tópico de entrada |
| `INVENTORY_RESERVED_TOPIC` | no | `inventory.reserved` | tópico de salida |
| `INVENTORY_REJECTED_TOPIC` | no | `inventory.rejected` | tópico de salida |
| `OUTBOX_POLL_INTERVAL` | no | `1s` | periodo del relay del outbox |
| `OUTBOX_BATCH_SIZE` | no | `100` | filas por ciclo del relay |
| `HTTP_PORT` | no | `8081` | puerto HTTP |
| `DB_SCHEMA` | no | `inventory` | esquema del servicio (se crea al arrancar) |
| `DB_POOL_MAX` | no | `16` | tamaño máximo del pool JDBC |
| `LOG_JSON` | no | `false` | logs en JSON |
| `LOG_LEVEL` | no | `INFO` | nivel de log |

Obligatorias en los perfiles `dev` y `prod`; en `test` e `it` las provee Dev Services.

## Calidad

```bash
make verify          # ./mvnw -B verify: tests, JaCoCo >= 80 % líneas / 70 % ramas, Spotless, Checkstyle
./mvnw spotless:apply
./mvnw -B verify -DskipITs=false   # además ejecuta StockFlowIT contra el paquete JVM
```

| Capa | Tipo de test |
|---|---|
| `domain`, `application.usecase` | JUnit 5 + AssertJ + fakes en memoria (sin Quarkus) |
| `adapter.in.rest` | `@QuarkusTest` + REST Assured + `@InjectMock` |
| `adapter.out.persistence` | `@QuarkusTest` + Dev Services + `@TestTransaction` |
| `adapter.*.messaging` | `@QuarkusTest` + `InMemoryConnector` |
| binario | `@QuarkusIntegrationTest` (`StockFlowIT`) |

## Imagen nativa (sin GraalVM en el host)

```bash
make build-native    # ./mvnw -B package -Dnative -DskipTests  (Mandrel dentro de Podman)
make image           # podman build -f src/main/docker/Containerfile.native-micro -t localhost/inventory-service:<tag> .
make run             # IMAGE=localhost/inventory-service:<tag> ./run.sh podman
```

`./mvnw verify -Dnative` ejecuta además `StockFlowIT` contra el binario nativo; como el binario es
Linux, ese paso solo funciona en un host Linux (o en CI). En Windows y macOS se compila con
`-DskipITs` y el binario se prueba dentro del contenedor.

En Windows el binario también corre en una distribución WSL, contra los pods de `./run.sh infra`:

```bash
./mvnw install -Dnative -DskipITs      # Git Bash: tests + binario Linux en target/
./run.sh native                        # Git Bash: lanza el binario en la distribución WSL por defecto
```

Desde Git Bash `run.sh` pasa las variables a WSL con `WSLENV`; `WSL_DISTRO=<nombre>` elige otra
distribución. Dentro de WSL los pods de Podman están en `localhost` (5433 y 9092); la IP de Windows
no sirve porque el firewall no expone esos puertos a WSL.

`curl localhost:8081/...` responde tanto desde WSL como desde Windows.

## Documentación

- [Contrato de eventos](docs/events.md)
- [ADRs](docs/adr/README.md)
