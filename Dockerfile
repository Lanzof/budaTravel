# Используем легковесный образ с Java 21
FROM eclipse-temurin:21-jre-alpine

# Рабочая директория внутри контейнера
WORKDIR /app

# Копируем собранный JAR-файл из папки api/build/libs
# Мы используем подстановку *, чтобы не привязываться к номеру версии
COPY api/build/libs/api-*.jar app.jar

# Команда запуска
ENTRYPOINT ["java", "-jar", "/app/app.jar"]