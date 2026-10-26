# Multi-stage Dockerfile for CampusConnect (Spring Boot 4.0 / Java 21)

# Stage 1: Build Application Artifact
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper configuration and dependencies descriptor
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies offline for fast layer caching
RUN ./mvnw dependency:go-offline -B || true

# Copy application source code and compile production JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Lightweight Production JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

# Expose default application port
EXPOSE 8081

# JVM memory configuration optimized for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
