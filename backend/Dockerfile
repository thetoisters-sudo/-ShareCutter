FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -DskipTests package


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]