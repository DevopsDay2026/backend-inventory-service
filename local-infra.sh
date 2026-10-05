#!/usr/bin/env bash
# PostgreSQL y Kafka locales, cada uno en su propio pod de Podman, para los modos dev, jvm y native.
# Las credenciales llegan por entorno desde run.sh; este script no contiene ningún valor sensible.
set -euo pipefail

# Git Bash convierte los argumentos que parecen rutas POSIX; aquí son rutas dentro del contenedor.
export MSYS_NO_PATHCONV=1

POSTGRES_POD="local-postgres"
# 5433 en el host: 5432 suele estar ocupado por un PostgreSQL instalado en la máquina.
POSTGRES_HOST_PORT=5433
KAFKA_POD="local-kafka"
POSTGRES_IMAGE="docker.io/library/postgres:16-alpine"
KAFKA_IMAGE="docker.io/apache/kafka:3.9.0"
KAFKA_CLUSTER_ID="MkU3OEVBNTcwNTJENDM2Qk"
READY_ATTEMPTS=60

wait_until() {
  local description="$1"
  shift
  for _ in $(seq "$READY_ATTEMPTS"); do
    if "$@" >/dev/null 2>&1; then
      echo "$description listo"
      return 0
    fi
    sleep 2
  done
  echo "$description no respondió a tiempo" >&2
  return 1
}

postgres_up() {
  if podman pod exists "$POSTGRES_POD"; then
    podman pod start "$POSTGRES_POD" >/dev/null
  else
    : "${DB_USERNAME:?define DB_USERNAME en run.sh}"
    : "${DB_PASSWORD:?define DB_PASSWORD en run.sh}"
    podman pod create --name "$POSTGRES_POD" -p "$POSTGRES_HOST_PORT:5432" >/dev/null
    POSTGRES_USER="$DB_USERNAME" POSTGRES_PASSWORD="$DB_PASSWORD" \
      podman run -d --pod "$POSTGRES_POD" --name "$POSTGRES_POD-db" \
      -e POSTGRES_USER -e POSTGRES_PASSWORD -e POSTGRES_DB=postgres \
      "$POSTGRES_IMAGE" >/dev/null
  fi
  wait_until "PostgreSQL (localhost:$POSTGRES_HOST_PORT)" \
    podman exec "$POSTGRES_POD-db" pg_isready -h localhost -d postgres
}

kafka_up() {
  if podman pod exists "$KAFKA_POD"; then
    podman pod start "$KAFKA_POD" >/dev/null
  else
    podman pod create --name "$KAFKA_POD" -p 9092:9092 >/dev/null
    podman run -d --pod "$KAFKA_POD" --name "$KAFKA_POD-broker" \
      -e KAFKA_NODE_ID=1 \
      -e KAFKA_PROCESS_ROLES=broker,controller \
      -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://localhost:9093 \
      -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
      -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT \
      -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
      -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
      -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT \
      -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
      -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
      -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
      -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
      -e CLUSTER_ID="$KAFKA_CLUSTER_ID" \
      "$KAFKA_IMAGE" >/dev/null
  fi
  wait_until "Kafka (localhost:9092)" \
    podman exec "$KAFKA_POD-broker" \
    /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
}

ACTION="${1:-up}"
case "$ACTION" in
  up)
    postgres_up
    kafka_up
    ;;
  down)
    podman pod rm -f --ignore "$POSTGRES_POD" "$KAFKA_POD"
    ;;
  status)
    podman pod ps --filter "name=local-"
    ;;
  *)
    echo "uso: $0 {up|down|status}" >&2
    exit 64
    ;;
esac
