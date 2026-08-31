# Layered image for the Meridian backend.
#
# Expects the boot jar to have been built already: ./gradlew bootJar
# CI builds it in the same job for that reason, so the jar is not compiled twice.

# ---- extract ---------------------------------------------------------------
# Spring Boot's layered jar splits into layers that change at different rates. Extracting them into
# separate image layers means a code change re-uploads the application layer only, not the ~100MB
# of dependencies underneath it.
FROM docker.io/library/eclipse-temurin:25-jre-alpine AS extract
WORKDIR /build
COPY build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

# ---- runtime ---------------------------------------------------------------
FROM docker.io/library/eclipse-temurin:25-jre-alpine

# Runs unprivileged. A container that does not need root should not have it, and this one holds
# payout details and identity documents (NFR-08).
RUN addgroup -S meridian && adduser -S -G meridian meridian

WORKDIR /app

# Ordered least- to most-frequently changed, so Docker's cache is useful.
COPY --from=extract --chown=meridian:meridian /build/extracted/dependencies/ ./
COPY --from=extract --chown=meridian:meridian /build/extracted/spring-boot-loader/ ./
COPY --from=extract --chown=meridian:meridian /build/extracted/snapshot-dependencies/ ./
COPY --from=extract --chown=meridian:meridian /build/extracted/application/ ./

USER meridian
EXPOSE 8080

# Kubernetes owns liveness and readiness through the actuator probes; this HEALTHCHECK is for
# running the image outside a cluster, where nothing else would notice a wedged process.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

# MaxRAMPercentage rather than a fixed -Xmx: the JVM sizes itself from the container memory limit,
# so the deployment manifest is the single place memory is configured.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
