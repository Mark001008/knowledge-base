FROM docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY knowledge-common/pom.xml knowledge-common/pom.xml
COPY knowledge-dal/pom.xml knowledge-dal/pom.xml
COPY knowledge-manager/pom.xml knowledge-manager/pom.xml
COPY knowledge-integration/pom.xml knowledge-integration/pom.xml
COPY knowledge-core/pom.xml knowledge-core/pom.xml
COPY knowledge-service/pom.xml knowledge-service/pom.xml
COPY knowledge-trigger/pom.xml knowledge-trigger/pom.xml
COPY knowledge-start/pom.xml knowledge-start/pom.xml
COPY knowledge-tests/pom.xml knowledge-tests/pom.xml

RUN mvn -B -pl knowledge-start -am -DskipTests dependency:go-offline

COPY . .
RUN mvn -B -pl knowledge-start -am -DskipTests package

FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre
WORKDIR /app

ENV JAVA_OPTS=""
COPY --from=build /workspace/knowledge-start/target/knowledge-start-0.0.1-SNAPSHOT.jar /app/knowledge-base.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/knowledge-base.jar"]
