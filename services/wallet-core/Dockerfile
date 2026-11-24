# Build context: repository root (see compose.yaml).
FROM eclipse-temurin:25.0.5.1_2-jdk-noble AS build
# The Maven wrapper needs unzip to install the verified Maven distribution.
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY services/wallet-core/pom.xml services/wallet-core/pom.xml
COPY services/wallet-core/src services/wallet-core/src
# Tests and checks run in CI before the image is built; here we only package.
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl services/wallet-core package -DskipTests -Dspotless.check.skip=true -Djacoco.skip=true

FROM eclipse-temurin:25.0.5.1_2-jre-noble
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /workspace/services/wallet-core/target/wallet-core-*.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
