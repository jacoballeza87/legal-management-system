#!/bin/bash
set -e  # Detener todo si algo falla

PROJECT_ID="jacob-504115"
REGION="us-central1"
REGISTRY="us-central1-docker.pkg.dev/${PROJECT_ID}/lms-services"

echo "════════════════════════════════════════"
echo "  1. Actualizando código desde GitHub"
echo "════════════════════════════════════════"
cd ~/legal-management-system
git pull

echo "════════════════════════════════════════"
echo "  2. Compilando y desplegando BACKEND"
echo "════════════════════════════════════════"
cd backend   # ── Contexto de build para TODOS los docker build de aquí en adelante

deploy_service () {
  local SERVICE=$1
  shift
  local EXTRA_ARGS="$@"

  echo "→ ${SERVICE}"
  # Compilar el jar dentro de la carpeta del servicio
  (cd "${SERVICE}" && mvn clean package -DskipTests)

  # Build/push CON CONTEXTO = backend/ (aquí es donde vivía el bug)
  docker build -t ${REGISTRY}/${SERVICE}:latest -f ${SERVICE}/Dockerfile .
  docker push ${REGISTRY}/${SERVICE}:latest

  gcloud run deploy ${SERVICE} \
    --image=${REGISTRY}/${SERVICE}:latest \
    --region=${REGION} --platform=managed --port=8080 \
    --memory=1Gi --cpu=1 --min-instances=0 --max-instances=5 \
    --allow-unauthenticated \
    ${EXTRA_ARGS}
}

# ── auth-service ──────────────────────────────────────────────
deploy_service auth-service

# ── api-gateway (necesita secrets/env vars) ─────────────────────
deploy_service api-gateway \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest,SPRING_DATA_REDIS_PASSWORD=upstash-redis-password:latest \
  --set-env-vars=SPRING_DATA_REDIS_HOST=adjusted-pigeon-41377.upstash.io,SPRING_DATA_REDIS_PORT=6379,SPRING_DATA_REDIS_SSL_ENABLED=true,CORS_ALLOWED_ORIGINS=https://frontend-308390111901.us-central1.run.app

# ── case-service ───────────────────────────────────────────────
deploy_service case-service

# ── user-service (necesita JWT_SECRET + datos de Cloud SQL) ─────
deploy_service user-service \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest

# ── document-service ───────────────────────────────────────────
deploy_service document-service

# ── notification-service ──────────────────────────────────────
deploy_service notification-service

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
