# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero el pom.xml para aprovechar la cache de capas de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ahora copiamos el resto del código y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos solo el jar generado en la etapa anterior (imagen final mucho más liviana)
COPY --from=build /app/target/*.jar app.jar

# Cloud Run inyecta la variable PORT; Spring Boot debe escuchar en ese puerto
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
