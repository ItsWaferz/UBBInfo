# syntax=docker/dockerfile:1

# ---- build stage: compile the Spring Boot jar (Java 17 target) ----
# Build with JDK 17 and skip tests: the app targets 17, and the test suite needs
# JDK-25 Mockito flags that aren't wanted in a container build.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Resolve dependencies first so this layer is cached across source-only changes.
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- run stage: small JRE image ----
FROM eclipse-temurin:17-jre
WORKDIR /app
# Liberation fonts so the generated PDFs (adeverințe, cereri, liste facilități)
# render with the intended serif face instead of a fallback.
RUN apt-get update \
    && apt-get install -y --no-install-recommends fonts-liberation \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
# Respect the container memory limit (Cloud Run --memory) instead of the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
# Cloud Run provides $PORT (default 8080); Spring reads it via server.port.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
