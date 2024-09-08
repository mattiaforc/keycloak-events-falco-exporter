# syntax=docker/dockerfile:1

ARG KC_VERSION="21.0.0"

FROM maven:3 AS builder

COPY pom.xml pom.xml
COPY src/ ./src/
ARG KC_VERSION
RUN mvn clean install -Dkeycloak.version=$KC_VERSION

FROM quay.io/keycloak/keycloak:${KC_VERSION}

COPY --from=builder target/spi-falco-event-[0-9].[0-9]-SNAPSHOT.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh --verbose build
