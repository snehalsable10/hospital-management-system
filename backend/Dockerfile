# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage
#
# JDK 17 to match maven.compiler.target in pom.xml, not whatever the base
# image happens to ship.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Dependencies resolve from pom.xml alone, so copying it first means a source
# change does not re-download the world. go-offline is allowed to fail: it is
# a warm-up, and the package step below is what actually has to succeed.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline || true

COPY src ./src

# Tests run in CI, where a failure is visible and reportable. Running them
# again here would double every image build for no extra signal.
RUN mvn -B -ntp clean package -DskipTests

# ---------------------------------------------------------------------------
# Runtime stage
#
# JRE rather than JDK: the compiler is not needed to run the jar, and leaving
# it out drops a few hundred megabytes and a chunk of attack surface.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

# curl is only here for the container healthcheck below.
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*

# Runs as a normal user. A container process that does not need root should
# not have it, in case anything escapes the JVM.
RUN groupadd --system hms && useradd --system --gid hms --create-home hms

WORKDIR /app

# The prod profile writes a log file; give it somewhere writable that is not
# the application directory.
RUN mkdir -p /var/log/hms && chown -R hms:hms /var/log/hms /app

COPY --from=build --chown=hms:hms /build/target/*.jar app.jar

USER hms

EXPOSE 8080

# MaxRAMPercentage instead of a fixed -Xmx: the JVM then sizes its heap from
# the container's memory limit rather than the host's total RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

# /actuator/health is permitAll in SecurityConfig, so this needs no token.
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
