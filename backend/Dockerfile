FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY auth/pom.xml auth/pom.xml
COPY organization/pom.xml organization/pom.xml
COPY course/pom.xml course/pom.xml
COPY ai/pom.xml ai/pom.xml
COPY migration/pom.xml migration/pom.xml
COPY app/pom.xml app/pom.xml
COPY auth/src auth/src
COPY organization/src organization/src
COPY course/src course/src
COPY ai/src ai/src
COPY migration/src migration/src
COPY app/src app/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -Dmaven.test.skip=true package
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S hacktrain && adduser -S hacktrain -G hacktrain
WORKDIR /app
COPY --from=build --chown=hacktrain:hacktrain /workspace/app/target/app-1.0.0-SNAPSHOT.jar app.jar
COPY --from=build --chown=hacktrain:hacktrain /workspace/migration/target/migration-1.0.0-SNAPSHOT.jar migrator.jar
USER hacktrain
EXPOSE 8080 9091
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-XX:+ExitOnOutOfMemoryError","-jar","app.jar"]
