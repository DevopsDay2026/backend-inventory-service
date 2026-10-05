# ADR-0004: Imagen nativa compilada en Podman con Mandrel

- Estado: Aceptado
- Fecha: 2026-10-03

## Contexto

Queremos arranque en milisegundos y poca memoria, sin pedir a cada desarrollador que instale
GraalVM ni depender de Docker.

## Decisión

- Perfil Maven `native` con `quarkus.native.container-build=true`,
  `quarkus.native.container-runtime=podman` y el builder
  `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21`.
- Imagen de ejecución: `src/main/docker/Containerfile.native-micro` sobre
  `quay.io/quarkus/ubi9-quarkus-micro-image`, usuario no root `1001` y grupo `0` con permisos de
  grupo, para que funcione con el UID arbitrario de OpenShift.
- `Makefile` con `build-native`, `image`, `run` y `verify`.
- Los records que Jackson (de)serializa fuera de JAX-RS llevan `@RegisterForReflection`.
- `./mvnw verify -Dnative` ejecuta `@QuarkusIntegrationTest` contra el binario.

## Consecuencias

- La compilación nativa tarda minutos y necesita unos 6 GB de memoria en la máquina de Podman.
- El binario es Linux: en macOS solo se ejecuta dentro del contenedor (`./run.sh podman`); en
  Windows también corre en una distribución WSL (`./run.sh native` desde la distribución).
- La misma imagen se etiqueta y se sube a cada registro; no se recompila por entorno.
