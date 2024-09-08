FROM maven:3 AS builder

COPY pom.xml pom.xml
COPY src/ ./src/
RUN mvn clean install

FROM quay.io/keycloak/keycloak:21.0.0

COPY --from=builder target/spi-falco-event-[0-9].[0-9]-SNAPSHOT.jar /opt/keycloak/providers/
RUN /opt/keycloak/bin/kc.sh --verbose build
