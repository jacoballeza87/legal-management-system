# ══════════════════════════════════════════════════
# STAGE 1: Build ALL modules
# ══════════════════════════════════════════════════
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM first (Docker cache layer)
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY case-service/pom.xml case-service/pom.xml
COPY document-service/pom.xml document-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

# Download dependencies (cached unless POMs change)
RUN mvn dependency:go-offline -B || true

# Copy source code and build
COPY . .
RUN mvn clean package -Dmaven.test.skip=true -B

# Stage JARs with clean names
RUN mkdir -p /staging && \
    find api-gateway/target -name "*.jar" ! -name "*-plain*" ! -name "*original*" -type f | head -1 | xargs -I {} cp {} /staging/api-gateway.jar && \
    find auth-service/target -name "*.jar" ! -name "*-plain*" ! -name "*original*" -type f | head -1 | xargs -I {} cp {} /staging/auth-service.jar && \
    find user-service/target -name "*.jar" ! -name "*-plain*" ! -name "*original*" -type f | head -1 | xargs -I {} cp {} /staging/user-service.jar && \
    find case-service/target -name "*.jar" ! -name "*-plain*" ! -name "*original*" -type f | head -1 | xargs -I {} cp {} /staging/case-service.jar && \
    find document-service/target -name "*.jar" ! -name "*-plain*" ! -name "*original*" -type f | head -1 | xargs -I {} cp {} /staging/document-service.jar && \
    find notification-service/target -name "*.jar" ! -name "*-plain*" ! -name "*original*" -type f | head -1 | xargs -I {} cp {} /staging/notification-service.jar

# ══════════════════════════════════════════════════
# STAGE 2: Runtime — lightweight image
# ══════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install bash (Alpine uses sh by default)
RUN apk add --no-cache bash curl

# Copy all service JARs
COPY --from=build /staging/api-gateway.jar .
COPY --from=build /staging/auth-service.jar .
COPY --from=build /staging/user-service.jar .
COPY --from=build /staging/case-service.jar .
COPY --from=build /staging/document-service.jar .
COPY --from=build /staging/notification-service.jar .

# Copy startup script
COPY start-all.sh .
RUN chmod +x start-all.sh

# Create directories
RUN mkdir -p /app/uploads /app/logs

# Expose gateway port
EXPOSE 8080

ENTRYPOINT ["./start-all.sh"]