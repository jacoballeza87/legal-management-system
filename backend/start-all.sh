#!/bin/bash

echo "═══════════════════════════════════════════════"
echo "  🏛️  Legal Management System"
echo "  Starting ALL services — staggered"
echo "═══════════════════════════════════════════════"

JVM_OPTS="-Xmx40m -Xms12m -XX:MaxMetaspaceSize=80m -XX:CompressedClassSpaceSize=24m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss256k"


RABBIT_SAFE="--spring.rabbitmq.host=localhost"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.port=5672"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.listener.simple.auto-startup=false"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.listener.direct.auto-startup=false"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.listener.simple.missing-queues-fatal=false"
RABBIT_SAFE="$RABBIT_SAFE --spring.rabbitmq.connection-timeout=1000"

BEAN_OVERRIDE="--spring.main.allow-bean-definition-overriding=true"

# ── Google Drive disabled (dummy values) ──
GDRIVE_OFF="--google.drive.credentials-path=/dev/null"
GDRIVE_OFF="$GDRIVE_OFF --google.drive.folder-id=disabled"
GDRIVE_OFF="$GDRIVE_OFF --google.drive.application-name=legal-system"

# ── AWS S3 disabled (dummy values) ──
S3_OFF="--aws.s3.bucket=disabled"
S3_OFF="$S3_OFF --aws.s3.region=us-east-1"
S3_OFF="$S3_OFF --aws.access-key-id=disabled"
S3_OFF="$S3_OFF --aws.secret-access-key=disabled"

echo ""
echo "════════════════════════════════════════"
echo "  Phase 1: Auth (most critical)"
echo "════════════════════════════════════════"

echo "[1/5] 🔐 Starting auth-service on port 8081..."
java $JVM_OPTS -jar auth-service.jar \
    --server.port=8081 \
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
    $RABBIT_SAFE \
    2>&1 | sed 's/^/[USER] /' &
USER_PID=$!

echo "⏳ Waiting 25s..."
sleep 25

echo "[3/5] 📁 Starting case-service on port 8083..."
java $JVM_OPTS -jar case-service.jar \
    --server.port=8083 \
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

java $JVM_OPTS -jar api-gateway.jar \
    --server.port=8080 \
    2>&1 | sed 's/^/[GATEWAY] /'