# ===== BUILD =====
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

# ===== RUNTIME =====
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

ADD --chown=app:app --checksum=sha256:86c97c1348277bb024f6185c4fd15a5567c50ab648f88277dffc429adb4bf927 \
    https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/1.68.1/dd-java-agent-1.68.1.jar dd-java-agent.jar
COPY --chown=app:app --from=build /app/target/*.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]