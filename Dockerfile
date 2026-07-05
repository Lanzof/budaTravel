FROM eclipse-temurin:21-jre-alpine

ARG JAR_FILE

RUN apk add --no-cache curl

WORKDIR /app
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
