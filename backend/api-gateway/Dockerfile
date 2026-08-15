FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./pom.xml
COPY api-gateway/pom.xml ./api-gateway/pom.xml
RUN mvn dependency:go-offline -B -f ./api-gateway/pom.xml

COPY api-gateway/src ./api-gateway/src
RUN mvn clean package -Dmaven.test.skip=true -f ./api-gateway/pom.xml

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/api-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
