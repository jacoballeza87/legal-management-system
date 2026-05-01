FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Parent POM
COPY pom.xml ./pom.xml

# Servicio específico
COPY user-service/pom.xml ./user-service/pom.xml
RUN mvn dependency:go-offline -B -f ./user-service/pom.xml

COPY user-service/src ./user-service/src
RUN mvn clean package -DskipTests -f ./user-service/pom.xml

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/user-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]