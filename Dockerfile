FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY telegrambots-shared ./telegrambots-shared
COPY telegrambots-config ./telegrambots-config
COPY telegrambots-mongo ./telegrambots-mongo
COPY telegrambots-activation ./telegrambots-activation
COPY telegrambots-bot ./telegrambots-bot
COPY telegrambots-telegram ./telegrambots-telegram
COPY telegrambots-notification ./telegrambots-notification
COPY telegrambots-github ./telegrambots-github
COPY telegrambots-admin ./telegrambots-admin
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
