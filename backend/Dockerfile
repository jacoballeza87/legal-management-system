# Build context: backend/ (proyecto Maven multi-módulo)
# docker build -f user-service/Dockerfile -t user-service .

FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
COPY user-service/pom.xml ./user-service/pom.xml
RUN mvn dependency:go-offline -B -f ./user-service/pom.xml

COPY user-service/src ./user-service/src
RUN mvn clean package -Dmaven.test.skip=true -f ./user-service/pom.xml

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/user-service/target/*.jar app.jar

# Puerto real: 8082 (definido en application.yml)
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
