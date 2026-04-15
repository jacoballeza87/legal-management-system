#!/bin/bash

echo "═══════════════════════════════════════════════"
echo "  🏛️  Legal Management System"
echo "  Starting ESSENTIAL services only"
echo "═══════════════════════════════════════════════"

# ── FIXED memory: more metaspace, less heap ──
# Each JVM: ~64MB heap + 128MB metaspace + ~30MB overhead ≈ 222MB
# 4 JVMs × 222MB ≈ 888MB total (fits in Railway)
JVM_OPTS="-Xmx64m -Xms32m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# ── Disable RabbitMQ completely ──
RABBIT_OFF="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"

echo ""
echo "════════════════════════════════════════"
echo "  Phase 1: Core services"
echo "════════════════════════════════════════"

echo "[1/4] 🔐 Starting auth-service on port 8081..."
java $JVM_OPTS -jar auth-service.jar \
    --server.port=8081 \
    $RABBIT_OFF \
    2>&1 | sed 's/^/[AUTH] /' &
AUTH_PID=$!

# ── Stagger starts to avoid memory spike ──
echo "⏳ Waiting 20s before next service..."
sleep 20

echo "[2/4] 👤 Starting user-service on port 8082..."
java $JVM_OPTS -jar user-service.jar \
    --server.port=8082 \
    $RABBIT_OFF \
    2>&1 | sed 's/^/[USER] /' &
USER_PID=$!

echo "⏳ Waiting 20s before next service..."
sleep 20

echo "[3/4] 📁 Starting case-service on port 8083..."
java $JVM_OPTS -jar case-service.jar \
    --server.port=8083 \
    $RABBIT_OFF \
    2>&1 | sed 's/^/[CASE] /' &
CASE_PID=$!

# ── Wait for services to fully initialize ──
echo ""
echo "⏳ Waiting 60s for services to fully start..."
sleep 60

# ── Health check ──
echo ""
echo "🏥 Health check:"
for entry in "8081:auth" "8082:user" "8083:case"; do
    port="${entry%%:*}"
    name="${entry##*:}"
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health 2>/dev/null)
    if [ "$STATUS" = "200" ]; then
        echo "   ✅ $name (port $port) — UP"
    else
        echo "   ⚠️  $name (port $port) — HTTP $STATUS"
    fi
done

# ── Process check ──
echo ""
echo "🔍 Process check:"
for pid_entry in "$AUTH_PID:auth" "$USER_PID:user" "$CASE_PID:case"; do
    pid="${pid_entry%%:*}"
    name="${pid_entry##*:}"
    if kill -0 "$pid" 2>/dev/null; then
        echo "   ✅ $name (PID $pid) — ALIVE"
    else
        echo "   ❌ $name (PID $pid) — DEAD"
    fi
done

# ── FREE MEMORY CHECK ──
echo ""
echo "💾 Memory usage:"
free -m 2>/dev/null || echo "   (free command not available)"
echo ""

# ════════════════════════════════════════
# Start API Gateway LAST in FOREGROUND
# ════════════════════════════════════════
echo "[4/4] 🌐 Starting api-gateway on port 8080..."
echo "═══════════════════════════════════════════════"

java $JVM_OPTS -jar api-gateway.jar \
    --server.port=8080 \
    2>&1 | sed 's/^/[GATEWAY] /'