# syntax=docker/dockerfile:1.7

FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /workspace

# Copy project files and build executable jar.
COPY . .
RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Run as non-root user for better container security.
RUN useradd --system --create-home --shell /usr/sbin/nologin spring

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080

USER spring
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

