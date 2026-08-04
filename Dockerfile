# Multi-stage build
# Stage 1: Build JAR with Maven
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/transaction-monitor-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD java -cp app.jar org.springframework.boot.loader.JarLauncher || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

