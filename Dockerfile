# ============ Stage 1: Builder ============
FROM amazoncorretto:17 AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon

# ============ Stage 2: Runtime ============
FROM amazoncorretto:17-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown -R app:app /app

USER app

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/actuator/health || exit 1

ENV JVM_OPTS=""

ENTRYPOINT ["sh", "-c", "java ${JVM_OPTS} -jar app.jar"]