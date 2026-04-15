#!/bin/bash

echo "═══════════════════════════════════════════════"
echo "  🏛️  Legal Management System"
echo "  Starting ALL services — LOGS VISIBLE"
echo "═══════════════════════════════════════════════"

# ── Memory flags (tuned for Railway ~1.5GB container) ──
JVM_SMALL="-Xmx80m -Xms40m -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m"
JVM_MED="-Xmx96m -Xms48m -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m"

# ── RabbitMQ safe flags ──
RABBIT="--spring.rabbitmq.listener.simple.missing-queues-fatal=false --spring.rabbitmq.connection-timeout=5000"
RABBIT_OFF="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"

# ═══════════════════════════════════════════════════
# Start services with VISIBLE output (prefixed)
# ═══════════════════════════════════════════════════

echo ""
echo "[1/6] 🔐 Starting auth-service on port 8081..."
java $JVM_MED -jar auth-service.jar \
    --server.port=8081 \
    $RABBIT_OFF \
    2>&1 | sed 's/^/[AUTH] /' &
AUTH_PID=$!

echo "[2/6] 👤 Starting user-service on port 8082..."
java $JVM_MED -jar user-service.jar \
    --server.port=8082 \
    $RABBIT_OFF \
    2>&1 | sed 's/^/[USER] /' &
USER_PID=$!

echo "[3/6] 📁 Starting case-service on port 8083..."
java $JVM_MED -jar case-service.jar \
    --server.port=8083 \
    $RABBIT_OFF \
    2>&1 | sed 's/^/[CASE] /' &
CASE_PID=$!

echo "[4/6] 📄 Starting document-service on port 8084..."
java $JVM_MED -jar document-service.jar \
    --server.port=8084 \
    $RABBIT_OFF \
    2>&1 | sed 's/^/[DOC] /' &
DOC_PID=$!

echo "[5/6] 🔔 Starting notification-service on port 8085..."
java $JVM_SMALL -jar notification-service.jar \
    --server.port=8085 \
    $RABBIT_OFF \
    2>&1 | sed 's/^/[NOTIF] /' &
NOTIF_PID=$!

# ── Wait longer — 5 JVMs need time ──
echo ""
echo "⏳ Waiting 60 seconds for backend services..."
sleep 60

# ── Health check ──
echo ""
echo "🏥 Health check:"
for port in 8081 8082 8083 8084 8085; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
    if [ "$STATUS" = "200" ]; then
        echo "   ✅ Port $port — UP"
    else
        echo "   ⚠️  Port $port — HTTP $STATUS"
    fi
done

# ── Check if any backend process died ──
echo ""
echo "🔍 Process check:"
for pid_name in "$AUTH_PID:auth" "$USER_PID:user" "$CASE_PID:case" "$DOC_PID:doc" "$NOTIF_PID:notif"; do
    pid="${pid_name%%:*}"
    name="${pid_name##*:}"
    if kill -0 "$pid" 2>/dev/null; then
        echo "   ✅ $name (PID $pid) — ALIVE"
    else
        echo "   ❌ $name (PID $pid) — DEAD (crashed!)"
    fi
done

# ═══════════════════════════════════════════════════
# Start API Gateway in FOREGROUND (keeps container alive)
# ═══════════════════════════════════════════════════
echo ""
echo "[6/6] 🌐 Starting api-gateway on port 8080 (FOREGROUND)..."
echo "═══════════════════════════════════════════════"

java $JVM_SMALL -jar api-gateway.jar \
    --server.port=8080 \
    2>&1 | sed 's/^/[GATEWAY] /'