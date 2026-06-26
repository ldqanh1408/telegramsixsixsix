FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/target/telegrambots-*.jar /app/app.jar

ENV PORT=8080
EXPOSE 8080

USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
