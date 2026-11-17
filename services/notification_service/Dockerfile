# ---- build stage: compiles the jar, so the host needs no JDK or Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime stage: JRE only, runs as a non-root user ----
FROM eclipse-temurin:17-jre-noble
WORKDIR /app

RUN sed -i 's|http://archive.ubuntu.com/ubuntu|https://archive.ubuntu.com/ubuntu|g; s|http://security.ubuntu.com/ubuntu|https://security.ubuntu.com/ubuntu|g' /etc/apt/sources.list.d/ubuntu.sources \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 waslah
COPY --from=build /app/target/notification_service-*.jar app.jar
USER waslah
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
