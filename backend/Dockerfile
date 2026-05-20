FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

ARG SOURCE_REPOSITORY=""
ARG VCS_REF=""

LABEL org.opencontainers.image.source=$SOURCE_REPOSITORY
LABEL org.opencontainers.image.revision=$VCS_REF

RUN groupadd --system spring \
    && useradd --system --gid spring --create-home spring

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS=""

EXPOSE 8080

USER spring

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
