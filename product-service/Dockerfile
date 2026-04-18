# ── Stage 1: Build ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy parent POM first — lets Docker cache the dependency layer separately
COPY pom.xml .
COPY ../pom.xml ../pom.xml 2>/dev/null || true

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy only the fat JAR from the build stage
COPY --from=builder /build/target/*.jar app.jar

# Tuned JVM flags for containers:
#   -XX:+UseContainerSupport  → respect cgroup memory limits (not host RAM)
#   -XX:MaxRAMPercentage=75   → use 75% of container memory for heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
