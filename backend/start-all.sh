#!/bin/bash

echo "═══════════════════════════════════════════════"
echo "  🏛️  Legal Management System"
echo "  Starting ALL services in one container"
echo "═══════════════════════════════════════════════"

# JVM memory flags — tuned for Railway container
# Total: ~540MB heap + overhead ≈ 1.2GB RAM
JVM_OPTS_SMALL="-Xmx80m -Xms40m -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m"
JVM_OPTS_MEDIUM="-Xmx96m -Xms48m -XX:+UseSerialGC -XX:MaxMetaspaceSize=64m"

# ── Common Spring flags to prevent RabbitMQ crash ──
RABBIT_SAFE="--spring.rabbitmq.listener.simple.missing-queues-fatal=false"
RABBIT_RETRY="--spring.rabbitmq.connection-timeout=5000"

echo ""
echo "[1/6] 🔐 Starting auth-service on port 8081..."
java $JVM_OPTS_MEDIUM -jar auth-service.jar \
    --server.port=8081 \
    $RABBIT_SAFE $RABBIT_RETRY \
    > /app/logs/auth-service.log 2>&1 &
AUTH_PID=$!
echo "       PID: $AUTH_PID"

echo "[2/6] 👤 Starting user-service on port 8082..."
java $JVM_OPTS_MEDIUM -jar user-service.jar \
    --server.port=8082 \
    $RABBIT_SAFE $RABBIT_RETRY \
    > /app/logs/user-service.log 2>&1 &
USER_PID=$!
echo "       PID: $USER_PID"

echo "[3/6] 📁 Starting case-service on port 8083..."
java $JVM_OPTS_MEDIUM -jar case-service.jar \
    --server.port=8083 \
    $RABBIT_SAFE $RABBIT_RETRY \
    > /app/logs/case-service.log 2>&1 &
CASE_PID=$!
echo "       PID: $CASE_PID"

echo "[4/6] 📄 Starting document-service on port 8084..."
java $JVM_OPTS_MEDIUM -jar document-service.jar \
    --server.port=8084 \
    $RABBIT_SAFE $RABBIT_RETRY \
    > /app/logs/document-service.log 2>&1 &
DOC_PID=$!
echo "       PID: $DOC_PID"

echo "[5/6] 🔔 Starting notification-service on port 8085..."
java $JVM_OPTS_SMALL -jar notification-service.jar \
    --server.port=8085 \
    $RABBIT_SAFE $RABBIT_RETRY \
    > /app/logs/notification-service.log 2>&1 &
NOTIF_PID=$!
echo "       PID: $NOTIF_PID"

# ── Wait for backend services to initialize ──
echo ""
echo "⏳ Waiting 30 seconds for backend services..."
sleep 30

# ── Health check backend services ──
echo ""
echo "🏥 Health check:"
for port in 8081 8082 8083 8084 8085; do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null | grep -q "200"; then
        echo "   ✅ Port $port — UP"
    else
        echo "   ⚠️  Port $port — still starting (OK, gateway will retry)"
    fi
done

# ── Start API Gateway in FOREGROUND (keeps container alive) ──
echo ""
echo "[6/6] 🌐 Starting api-gateway on port 8080 (FOREGROUND)..."
echo "═══════════════════════════════════════════════"
echo ""

java $JVM_OPTS_SMALL -jar api-gateway.jar \
    --server.port=8080