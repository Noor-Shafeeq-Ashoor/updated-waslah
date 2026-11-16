# ---- build stage: compiles the jar, so the host needs no JDK or Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime stage: JRE only, runs as a non-root user ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 waslah
COPY --from=build /app/target/identity_service-*.jar app.jar
USER waslah
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
