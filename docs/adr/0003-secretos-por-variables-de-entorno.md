# ADR-0003: Secretos solo por variables de entorno

- Estado: Aceptado
- Fecha: 2026-10-03

## Contexto

"En mi máquina funciona" suele ser configuración distinta entre entornos. La misma imagen y el mismo
YAML deben correr en Podman, OpenShift y AWS cambiando únicamente variables de entorno.

## Decisión

- `application.properties` solo tiene valores no sensibles y placeholders `${ENV_VAR}`.
- Los secretos (`DB_PASSWORD`, `KAFKA_SASL_JAAS_CONFIG`) y los datos del entorno (`DB_URL`,
  `DB_USERNAME`, `KAFKA_BOOTSTRAP_SERVERS`) no tienen valor por defecto: si faltan, la aplicación no
  arranca.
- En local los valores salen de `run.sh` (ignorado por git); `run.sh.example` se commitea con los
  mismos nombres y valores falsos. En clúster salen de `ConfigMap` y `Secret` vía `envFrom`.
- La configuración se lee con `@ConfigMapping` en `adapter.config`. `System.getenv` está prohibido
  (regla de Checkstyle).
- Nada sensible en `-D` de compilación, `ENV` del Containerfile, manifiestos ni logs.

## Consecuencias

- En `test` e `it` no hace falta ninguna variable: Dev Services levanta PostgreSQL y Kafka en
  Podman. Las propiedades con placeholder obligatorio están bajo los perfiles `%dev,prod`.
- En `dev` Dev Services está desactivado: la aplicación usa el PostgreSQL y el Kafka que levanta
  `./run.sh infra` (pods de Podman), con las mismas variables que el binario nativo.
- Rotar un secreto es cambiar una variable y reiniciar; no se reconstruye la imagen.
