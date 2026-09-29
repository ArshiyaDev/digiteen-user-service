FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B  dependency:go-offline
COPY src src
RUN mvn -B -ntp verify

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/keys && chown -R spring:spring /app
USER spring:spring
WORKDIR /app
COPY --from=build /workspace/target/digiteen-user-service-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
