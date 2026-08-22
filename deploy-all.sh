#!/bin/bash
set -e  # Detener todo si algo falla

PROJECT_ID="jacob-504115"
REGION="us-central1"
REGISTRY="us-central1-docker.pkg.dev/${PROJECT_ID}/lms-services"

echo "════════════════════════════════════════"
echo "  1. Actualizando código desde GitHub"
echo "════════════════════════════════════════"
git pull

echo "════════════════════════════════════════"
echo "  2. Compilando y desplegando BACKEND"
echo "════════════════════════════════════════"
cd backend

# ── auth-service ──────────────────────────────────────────────
echo "→ auth-service"
cd auth-service
mvn clean package -DskipTests
docker build -t ${REGISTRY}/auth-service:latest .
docker push ${REGISTRY}/auth-service:latest
gcloud run deploy auth-service \
  --image=${REGISTRY}/auth-service:latest \
  --region=${REGION} --platform=managed --port=8080 \
  --memory=1Gi --cpu=1 --min-instances=0 --max-instances=5 \
  --allow-unauthenticated
cd ..

# ── api-gateway ────────────────────────────────────────────────
echo "→ api-gateway"
cd api-gateway
mvn clean package -DskipTests
docker build -t ${REGISTRY}/api-gateway:latest .
docker push ${REGISTRY}/api-gateway:latest
gcloud run deploy api-gateway \
  --image=${REGISTRY}/api-gateway:latest \
  --region=${REGION} --platform=managed --port=8080 \
  --memory=1Gi --cpu=1 --min-instances=0 --max-instances=5 \
  --allow-unauthenticated \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest,SPRING_DATA_REDIS_PASSWORD=upstash-redis-password:latest \
  --set-env-vars=SPRING_DATA_REDIS_HOST=adjusted-pigeon-41377.upstash.io,SPRING_DATA_REDIS_PORT=6379,SPRING_DATA_REDIS_SSL_ENABLED=true,CORS_ALLOWED_ORIGINS=https://frontend-308390111901.us-central1.run.app
cd ..

# ── case-service ───────────────────────────────────────────────
echo "→ case-service"
cd case-service
mvn clean package -DskipTests
docker build -t ${REGISTRY}/case-service:latest .
docker push ${REGISTRY}/case-service:latest
gcloud run deploy case-service \
  --image=${REGISTRY}/case-service:latest \
  --region=${REGION} --platform=managed --port=8080 \
  --memory=1Gi --cpu=1 --min-instances=0 --max-instances=5 \
  --allow-unauthenticated
cd ..

# ── user-service ───────────────────────────────────────────────
echo "→ user-service"
cd user-service
mvn clean package -DskipTests
docker build -t ${REGISTRY}/user-service:latest .
docker push ${REGISTRY}/user-service:latest
gcloud run deploy user-service \
  --image=${REGISTRY}/user-service:latest \
  --region=${REGION} --platform=managed --port=8080 \
  --memory=1Gi --cpu=1 --min-instances=0 --max-instances=5 \
  --allow-unauthenticated
cd ..

# ── document-service ───────────────────────────────────────────
echo "→ document-service"
cd document-service
mvn clean package -DskipTests
docker build -t ${REGISTRY}/document-service:latest .
docker push ${REGISTRY}/document-service:latest
gcloud run deploy document-service \
  --image=${REGISTRY}/document-service:latest \
  --region=${REGION} --platform=managed --port=8080 \
  --memory=1Gi --cpu=1 --min-instances=0 --max-instances=5 \
  --allow-unauthenticated
cd ..

# ── notification-service ──────────────────────────────────────
echo "→ notification-service"
cd notification-service
mvn clean package -DskipTests
docker build -t ${REGISTRY}/notification-service:latest .
docker push ${REGISTRY}/notification-service:latest
gcloud run deploy notification-service \
  --image=${REGISTRY}/notification-service:latest \
  --region=${REGION} --platform=managed --port=8080 \
  --memory=1Gi --cpu=1 --min-instances=0 --max-instances=5 \
  --allow-unauthenticated
cd ..

cd ..  # volver a la raíz del monorepo

echo "════════════════════════════════════════"
echo "  3. Compilando y desplegando FRONTEND"
echo "════════════════════════════════════════"
cd frontend
docker build -t ${REGISTRY}/frontend:latest .
docker push ${REGISTRY}/frontend:latest
gcloud run deploy frontend \
  --image=${REGISTRY}/frontend:latest \
  --region=${REGION} --platform=managed --port=80 \
  --memory=512Mi --cpu=1 --min-instances=0 --max-instances=5 \
  --allow-unauthenticated
cd ..

echo "════════════════════════════════════════"
echo "  ✅ DEPLOY COMPLETO"
echo "════════════════════════════════════════"