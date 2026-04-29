#!/bin/bash

echo "═══════════════════════════════════════════════"
echo "  🏛️  Legal Management System"
echo "  Starting ALL services — staggered"
echo "═══════════════════════════════════════════════"

JVM_OPTS="-Xmx55m -Xms20m -XX:MaxMetaspaceSize=60m -XX:CompressedClassSpaceSize=20m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k -noverify"

# ── DB connection (reads from Railway env vars) ──
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-legal_management_db}"
DB_USER="${DB_USER:-${MYSQLUSER:-root}}"
DB_PASSWORD="${DB_PASSWORD:-${MYSQLPASSWORD:-${MYSQL_ROOT_PASSWORD:-root}}}"

DB_ARGS="--spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
DB_ARGS="$DB_ARGS --spring.datasource.username=${DB_USER:-${MYSQLUSER:-root}}"
DB_ARGS="$DB_ARGS --spring.datasource.password=${DB_PASSWORD:-${MYSQLPASSWORD:-${MYSQL_ROOT_PASSWORD:-root}}}"
DB_ARGS="$DB_ARGS --spring.jpa.hibernate.ddl-auto=update"
DB_ARGS="$DB_ARGS --spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"

# ── Redis connection ──
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

REDIS_ARGS="--spring.data.redis.host=${REDIS_HOST}"
REDIS_ARGS="$REDIS_ARGS --spring.data.redis.port=${REDIS_PORT}"
if [ -n "$REDIS_PASSWORD" ]; then
  REDIS_ARGS="$REDIS_ARGS --spring.data.redis.password=${REDIS_PASSWORD}"
fi

# ── RabbitMQ ──
RABBIT_HOST="${SPRING_RABBITMQ_HOST:-localhost}"
RABBIT_PORT="${SPRING_RABBITMQ_PORT:-5672}"
RABBIT_USER="${SPRING_RABBITMQ_USERNAME:-guest}"
RABBIT_PASS="${SPRING_RABBITMQ_PASSWORD:-guest}"

RABBIT_SAFE="--spring.rabbitmq.host=${RABBIT_HOST}"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.port=${RABBIT_PORT}"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.username=${RABBIT_USER}"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.password=${RABBIT_PASS}"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.listener.simple.auto-startup=false"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.listener.direct.auto-startup=false"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.listener.simple.missing-queues-fatal=false"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.connection-timeout=1000"

BEAN_OVERRIDE="--spring.main.allow-bean-definition-overriding=true"

# ── Google Drive disabled ──
GDRIVE_OFF="--google.drive.credentials-path=/dev/null"
GDRIVE_OFF="$GDRIVE_OFF --google.drive.folder-id=disabled"
GDRIVE_OFF="$GDRIVE_OFF --google.drive.application-name=legal-system"

# ── AWS S3 disabled ──
S3_OFF="--aws.s3.bucket=disabled"
S3_OFF="$S3_OFF --aws.s3.region=us-east-1"
S3_OFF="$S3_OFF --aws.access-key-id=disabled"
S3_OFF="$S3_OFF --aws.secret-access-key=disabled"

echo ""
echo "🔌 DB: ${DB_HOST}:${DB_PORT}/${DB_NAME} (user: ${DB_USER})"
echo "🔴 Redis: ${REDIS_HOST}:${REDIS_PORT}"
echo "🐰 RabbitMQ: ${RABBIT_HOST}:${RABBIT_PORT}"
echo ""

echo "════════════════════════════════════════"
echo "  Phase 1: Auth (most critical)"
echo "════════════════════════════════════════"

echo "[1/5] 🔐 Starting auth-service on port 8081..."
java $JVM_OPTS -jar auth-service.jar \
    --server.port=8081 \
    $DB_ARGS \
    $REDIS_ARGS \
    $RABBIT_SAFE \
    --management.health.redis.enabled=false \
    2>&1 | sed 's/^/[AUTH] /' &
AUTH_PID=$!

echo "⏳ Waiting 25s for auth to initialize..."
sleep 25

echo ""
echo "════════════════════════════════════════"
echo "  Phase 2: User + Case"
echo "════════════════════════════════════════"

echo "[2/5] 👤 Starting user-service on port 8082..."
java $JVM_OPTS -jar user-service.jar \
    --server.port=8082 \
    $DB_ARGS \
    $RABBIT_SAFE \
    2>&1 | sed 's/^/[USER] /' &
USER_PID=$!

echo "⏳ Waiting 25s..."
sleep 25

echo "[3/5] 📁 Starting case-service on port 8083..."
java $JVM_OPTS -jar case-service.jar \
    --server.port=8083 \
    $DB_ARGS \
    $RABBIT_SAFE \
    --management.health.rabbit.enabled=false \
    2>&1 | sed 's/^/[CASE] /' &
CASE_PID=$!

echo "⏳ Waiting 25s..."
sleep 25

echo ""
echo "════════════════════════════════════════"
echo "  Phase 3: Document + Notification"
echo "════════════════════════════════════════"

echo "[4/5] 📄 Starting document-service on port 8084..."
java $JVM_OPTS -jar document-service.jar \
    --server.port=8084 \
    $DB_ARGS \
    $RABBIT_SAFE \
    $BEAN_OVERRIDE \
    $GDRIVE_OFF \
    $S3_OFF \
    --google.drive.enabled=false \
    --aws.enabled=false \
    --management.health.rabbit.enabled=false \
    2>&1 | sed 's/^/[DOC] /' &
DOC_PID=$!

echo "⏳ Waiting 25s..."
sleep 25

echo "[5/5] 🔔 Starting notification-service on port 8085..."
java $JVM_OPTS -jar notification-service.jar \
    --server.port=8085 \
    $DB_ARGS \
    $RABBIT_SAFE \
    $BEAN_OVERRIDE \
    --management.health.rabbit.enabled=false \
    --management.health.mail.enabled=false \
    2>&1 | sed 's/^/[NOTIF] /' &
NOTIF_PID=$!

echo "⏳ Waiting 30s for all services to finish starting..."
sleep 30

# ══════════════════════════════════════════
# Health check
# ══════════════════════════════════════════
echo ""
echo "🏥 Health check:"
for entry in "8081:auth" "8082:user" "8083:case" "8084:doc" "8085:notif"; do
    port="${entry%%:*}"
    name="${entry##*:}"
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
    if [ "$STATUS" = "200" ]; then
        echo "   ✅ $name (port $port) — UP"
    else
        echo "   ⚠️  $name (port $port) — HTTP $STATUS"
    fi
done

echo ""
echo "🔍 Process check:"
for pid_entry in "$AUTH_PID:auth" "$USER_PID:user" "$CASE_PID:case" "$DOC_PID:doc" "$NOTIF_PID:notif"; do
    pid="${pid_entry%%:*}"
    name="${pid_entry##*:}"
    if kill -0 "$pid" 2>/dev/null; then
        echo "   ✅ $name (PID $pid) — ALIVE"
    else
        echo "   ❌ $name (PID $pid) — DEAD"
    fi
done

echo ""
echo "💾 Memory:"
free -m 2>/dev/null || echo "   n/a"

# ══════════════════════════════════════════
# API Gateway LAST — keeps container alive
# ══════════════════════════════════════════
echo ""
echo "[GATEWAY] 🌐 Starting api-gateway on port 8080..."
echo "═══════════════════════════════════════════════"

JWT_SECRET="${JWT_SECRET:-mySecretKey2024LegalSystemChangeInProduction}"
CORS_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:4200}"

java $JVM_OPTS -jar api-gateway.jar \
    --server.port=8080 \
    $REDIS_ARGS \
    --jwt.secret=${JWT_SECRET} \
    --cors.allowed-origins=${CORS_ORIGINS} \
    2>&1 | sed 's/^/[GATEWAY] /'