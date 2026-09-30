FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY patient-service/pom.xml patient-service/
COPY patient-service/src patient-service/src
# The five images share one Maven cache, one build at a time.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    ./mvnw --batch-mode --no-transfer-progress --file patient-service package \
        -Dmaven.test.skip=true -Dspotless.check.skip=true
RUN java -Djarmode=tools -jar patient-service/target/patient-service-0.0.1-SNAPSHOT.jar \
        extract --layers --application-filename application.jar --destination extracted

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S mediscreen && adduser -S -G mediscreen mediscreen
WORKDIR /app
# Least changed first: a code change rebuilds only the last layer.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./
USER mediscreen
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --start-interval=2s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "application.jar"]
