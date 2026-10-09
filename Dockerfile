FROM gradle:8.7-jdk17 AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/recomind-all.jar app.jar
RUN mkdir -p /app/data
EXPOSE 10000
CMD ["java", "-jar", "app.jar"]
