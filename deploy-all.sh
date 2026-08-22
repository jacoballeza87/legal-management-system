#!/bin/bash
set -e  # Detener todo si algo falla

PROJECT_ID="jacob-504115"
REGION="us-central1"
REGISTRY="us-central1-docker.pkg.dev/${PROJECT_ID}/lms-services"

# ── Valores compartidos (ajusta si cambian) ─────────────────────────────────
DB_HOST="34.41.188.59"
DB_PORT="3306"
DB_NAME="legal_management_db"
DB_USER="root"
REDIS_HOST="adjusted-pigeon-41377.upstash.io"
REDIS_PORT="6379"
RABBITMQ_HOST="albatross.rmq.cloudamqp.com"
RABBITMQ_PORT="5671"
RABBITMQ_USERNAME="aaiijkge"
RABBITMQ_VHOST="aaiijkge"
FRONTEND_URL="https://frontend-308390111901.us-central1.run.app"

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
  local PORT=$2
  shift 2
  local EXTRA_ARGS="$@"

  echo "→ ${SERVICE} (puerto ${PORT})"
  (cd "${SERVICE}" && mvn clean package -DskipTests)

  docker build -t ${REGISTRY}/${SERVICE}:latest -f ${SERVICE}/Dockerfile .
  docker push ${REGISTRY}/${SERVICE}:latest

  gcloud run deploy ${SERVICE} \
    --image=${REGISTRY}/${SERVICE}:latest \
    --region=${REGION} --platform=managed --port=${PORT} \
    --memory=1Gi --cpu=1 --min-instances=0 --max-instances=5 \
    --allow-unauthenticated \
    ${EXTRA_ARGS}
}

# ── auth-service (8081) ──────────────────────────────────────────
deploy_service auth-service 8081 \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest,SPRING_DATA_REDIS_PASSWORD=upstash-redis-password:latest,DB_PASSWORD=TU_SECRET_DB_PASSWORD:latest \
  --set-env-vars=DB_HOST=${DB_HOST},DB_PORT=${DB_PORT},DB_NAME=${DB_NAME},DB_USER=${DB_USER},REDIS_HOST=${REDIS_HOST},REDIS_PORT=${REDIS_PORT}

# ── api-gateway (8080) ───────────────────────────────────────────
deploy_service api-gateway 8080 \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest,SPRING_DATA_REDIS_PASSWORD=upstash-redis-password:latest \
  --set-env-vars=SPRING_DATA_REDIS_HOST=${REDIS_HOST},SPRING_DATA_REDIS_PORT=${REDIS_PORT},SPRING_DATA_REDIS_SSL_ENABLED=true,CORS_ALLOWED_ORIGINS=${FRONTEND_URL}

# ── user-service (8082) ──────────────────────────────────────────
deploy_service user-service 8082 \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest,DB_PASSWORD=TU_SECRET_DB_PASSWORD:latest \
  --set-env-vars=DB_HOST=${DB_HOST},DB_PORT=${DB_PORT},DB_NAME=${DB_NAME},DB_USER=${DB_USER}

# ── case-service (8083) ───────────────────────────────────────────
deploy_service case-service 8083 \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest,DB_PASSWORD=TU_SECRET_DB_PASSWORD:latest \
  --set-env-vars=DB_HOST=${DB_HOST},DB_PORT=${DB_PORT},DB_NAME=${DB_NAME},DB_USER=${DB_USER},QR_BASE_URL=${FRONTEND_URL}

# ── notification-service (8084) ────────────────────────────────────
deploy_service notification-service 8084 \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest,DB_PASSWORD=TU_SECRET_DB_PASSWORD:latest,RABBITMQ_PASSWORD=cloudamqp-password:latest,MAIL_PASSWORD=TU_SECRET_MAIL_PASSWORD:latest,TWILIO_AUTH_TOKEN=TU_SECRET_TWILIO_TOKEN:latest \
  --set-env-vars=DB_HOST=${DB_HOST},DB_PORT=${DB_PORT},DB_NAME=${DB_NAME},DB_USER=${DB_USER},RABBITMQ_HOST=${RABBITMQ_HOST},RABBITMQ_PORT=${RABBITMQ_PORT},RABBITMQ_USERNAME=${RABBITMQ_USERNAME},RABBITMQ_VHOST=${RABBITMQ_VHOST},RABBITMQ_SSL_ENABLED=true,MAIL_HOST=TU_MAIL_HOST,MAIL_PORT=TU_MAIL_PORT,MAIL_USERNAME=TU_MAIL_USER,MAIL_FROM_EMAIL=TU_MAIL_FROM,MAIL_FROM_NAME="Legal Management System",TWILIO_ENABLED=false

# ── document-service (8085) ─────────────────────────────────────────
deploy_service document-service 8085 \
  --set-secrets=JWT_SECRET=auth-jwt-secret:latest,DB_PASSWORD=TU_SECRET_DB_PASSWORD:latest,RABBITMQ_PASSWORD=cloudamqp-password:latest \
  --set-env-vars=DB_HOST=${DB_HOST},DB_PORT=${DB_PORT},DB_NAME=${DB_NAME},DB_USER=${DB_USER},RABBITMQ_HOST=${RABBITMQ_HOST},RABBITMQ_PORT=${RABBITMQ_PORT},RABBITMQ_USERNAME=${RABBITMQ_USERNAME},RABBITMQ_VHOST=${RABBITMQ_VHOST},RABBITMQ_SSL_ENABLED=true,DOCUMENT_BASE_URL=https://document-service-XXXX.us-central1.run.app

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
