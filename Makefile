IMAGE ?= localhost/inventory-service
TAG   ?= $(shell git rev-parse --short HEAD 2>/dev/null || echo dev)

.PHONY: verify build-native image run

verify:
	./mvnw -B verify

build-native:
	./mvnw -B package -Dnative -DskipTests

image: build-native
	podman build -f src/main/docker/Containerfile.native-micro -t $(IMAGE):$(TAG) .

run:
	IMAGE=$(IMAGE):$(TAG) ./run.sh podman
