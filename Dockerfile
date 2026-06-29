FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY telegrambots-domain ./telegrambots-domain
COPY telegrambots-application ./telegrambots-application
COPY telegrambots-infrastructure ./telegrambots-infrastructure
COPY telegrambots-web ./telegrambots-web
COPY telegrambots-app ./telegrambots-app

RUN mvn -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/telegrambots-app/target/telegrambots-app-*.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080

USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
