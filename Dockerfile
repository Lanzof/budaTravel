FROM eclipse-temurin:21-jdk-alpine AS build

ARG MODULE

WORKDIR /workspace
COPY . .
RUN ./gradlew :${MODULE}:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

ARG MODULE

RUN apk add --no-cache curl

WORKDIR /app
COPY --from=build /workspace/${MODULE}/build/libs/${MODULE}-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
