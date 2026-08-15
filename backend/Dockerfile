# ---------- Etapa 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos el POM padre y el pom.xml de CADA módulo (Maven necesita verlos
# para entender la estructura del proyecto, aunque no vaya a compilarlos todos)
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY case-service/pom.xml case-service/pom.xml
COPY document-service/pom.xml document-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

# Descarga de dependencias en cache (acelera builds futuros)
RUN mvn -pl auth-service -am dependency:go-offline -B

# Ahora sí copiamos el código fuente real de auth-service
COPY auth-service/src auth-service/src

# Compilamos SOLO el módulo auth-service (-pl) y lo que necesite (-am)
RUN mvn -pl auth-service -am clean package -DskipTests -B

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/auth-service/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
