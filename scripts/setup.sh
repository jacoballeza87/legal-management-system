#!/bin/bash

# ============================================
# LEGAL MANAGEMENT SYSTEM - SETUP SCRIPT
# Script de configuración inicial del proyecto
# ============================================

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  Sistema de Gestión Legal y Contable${NC}"
echo -e "${GREEN}  Script de Configuración Inicial${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""

# Función para imprimir mensajes
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Verificar si el script se ejecuta como root
if [ "$EUID" -eq 0 ]; then 
    print_error "No ejecutes este script como root"
    exit 1
fi

# ============================================
# 1. VERIFICAR HERRAMIENTAS NECESARIAS
# ============================================

print_info "Verificando herramientas necesarias..."

check_command() {
    if command -v $1 &> /dev/null; then
        print_info "✓ $1 está instalado"
        return 0
    else
        print_warn "✗ $1 NO está instalado"
        return 1
    fi
}

MISSING_TOOLS=0

check_command "java" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "mvn" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "node" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "npm" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "ng" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "docker" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "docker-compose" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "kubectl" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "aws" || MISSING_TOOLS=$((MISSING_TOOLS+1))
check_command "terraform" || MISSING_TOOLS=$((MISSING_TOOLS+1))

if [ $MISSING_TOOLS -gt 0 ]; then
    print_error "Faltan $MISSING_TOOLS herramientas. Por favor instálalas antes de continuar."
    print_info "Consulta docs/INSTALLATION.md para instrucciones de instalación."
    exit 1
fi

echo ""
print_info "Todas las herramientas necesarias están instaladas ✓"
echo ""

# ============================================
# 2. VERIFICAR VERSIONES
# ============================================

print_info "Verificando versiones..."

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ $JAVA_VERSION -lt 17 ]; then
    print_error "Java 17 o superior es requerido. Versión actual: $JAVA_VERSION"
    exit 1
fi
print_info "✓ Java version: $JAVA_VERSION"

NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ $NODE_VERSION -lt 18 ]; then
    print_error "Node.js 18 o superior es requerido. Versión actual: $NODE_VERSION"
    exit 1
fi
print_info "✓ Node.js version: $NODE_VERSION"

echo ""

# ============================================
# 3. CREAR ESTRUCTURA DE DIRECTORIOS
# ============================================

print_info "Creando estructura de directorios..."

mkdir -p backend/{config-server,eureka-server,api-gateway,auth-service,user-service,case-service,notification-service,document-service}/src/{main/{java/com/legal/{config,controller,service,repository,model,dto,security,exception,util},resources},test/java}
mkdir -p frontend/legal-app
mkdir -p database/{migrations,scripts}
mkdir -p infrastructure/{kubernetes,terraform,docker,monitoring}
mkdir -p docs/{api,architecture,guides}
mkdir -p scripts/{deployment,setup,utilities}
mkdir -p config

print_info "✓ Estructura de directorios creada"
echo ""

# ============================================
# 4. CREAR ARCHIVO .env
# ============================================

print_info "Configurando variables de entorno..."

if [ ! -f .env ]; then
    print_info "Creando archivo .env..."
    
    cat > .env <<EOF
# ===========================================
# LEGAL MANAGEMENT SYSTEM - ENVIRONMENT VARS
# ===========================================

# ENVIRONMENT
ENVIRONMENT=development

# DATABASE
DB_HOST=localhost
DB_PORT=3306
DB_NAME=legal_management_db
DB_USER=legal_app
DB_PASSWORD=legal_password

# JWT
JWT_SECRET=change_this_to_a_very_secure_random_string_of_at_least_256_bits
JWT_EXPIRATION=86400000

# AWS CREDENTIALS (dejar vacío para desarrollo local)
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=us-east-1
AWS_S3_BUCKET=legal-system-admin-private

# AWS SES (Email)
AWS_SES_FROM_EMAIL=noreply@example.com

# AWS SNS
AWS_SNS_TOPIC_ARN=

# GOOGLE DRIVE API
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://localhost:4200/auth/google/callback
GOOGLE_SERVICE_ACCOUNT_PATH=./config/service-account.json

# MICROSOFT OAUTH
MICROSOFT_CLIENT_ID=
MICROSOFT_CLIENT_SECRET=

# TWILIO (SMS)
TWILIO_ACCOUNT_SID=
TWILIO_AUTH_TOKEN=
TWILIO_PHONE_NUMBER=

# KAFKA
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# REDIS
REDIS_HOST=localhost
REDIS_PORT=6379

# MICROSERVICES PORTS
CONFIG_SERVER_PORT=8888
EUREKA_SERVER_PORT=8761
API_GATEWAY_PORT=8080
AUTH_SERVICE_PORT=8081
USER_SERVICE_PORT=8082
CASE_SERVICE_PORT=8083
NOTIFICATION_SERVICE_PORT=8084
DOCUMENT_SERVICE_PORT=8085

# FRONTEND
FRONTEND_PORT=4200
FRONTEND_API_URL=http://localhost:8080/api
EOF

    print_info "✓ Archivo .env creado"
    print_warn "IMPORTANTE: Edita el archivo .env con tus credenciales reales"
else
    print_info "✓ Archivo .env ya existe"
fi

echo ""

# ============================================
# 5. INSTALAR DEPENDENCIAS DEL FRONTEND
# ============================================

read -p "¿Deseas instalar las dependencias del frontend ahora? (s/n): " install_frontend
if [ "$install_frontend" = "s" ] || [ "$install_frontend" = "S" ]; then
    if [ -f "frontend/legal-app/package.json" ]; then
        print_info "Instalando dependencias del frontend..."
        cd frontend/legal-app
        npm install
        cd ../..
        print_info "✓ Dependencias del frontend instaladas"
    else
        print_warn "package.json no encontrado. Primero genera el proyecto Angular."
    fi
fi

echo ""

# ============================================
# 6. COMPILAR BACKEND
# ============================================

read -p "¿Deseas compilar el backend ahora? (s/n): " compile_backend
if [ "$compile_backend" = "s" ] || [ "$compile_backend" = "S" ]; then
    if [ -f "backend/pom.xml" ]; then
        print_info "Compilando backend (esto puede tardar varios minutos)..."
        cd backend
        mvn clean install -DskipTests
        cd ..
        print_info "✓ Backend compilado exitosamente"
    else
        print_warn "pom.xml no encontrado. Asegúrate de que el backend esté configurado."
    fi
fi

echo ""

# ============================================
# 7. CONFIGURAR DOCKER
# ============================================

print_info "Verificando Docker..."

if docker info &> /dev/null; then
    print_info "✓ Docker está funcionando correctamente"
    
    read -p "¿Deseas levantar los servicios con Docker Compose? (s/n): " start_docker
    if [ "$start_docker" = "s" ] || [ "$start_docker" = "S" ]; then
        print_info "Iniciando servicios con Docker Compose..."
        docker-compose up -d mysql redis zookeeper kafka
        print_info "✓ Servicios de infraestructura iniciados"
        print_info "Esperando a que los servicios estén listos..."
        sleep 10
    fi
else
    print_warn "Docker no está corriendo. Inicia Docker Desktop y vuelve a ejecutar este script."
fi

echo ""

# ============================================
# 8. CONFIGURAR BASE DE DATOS
# ============================================

read -p "¿Deseas configurar la base de datos ahora? (s/n): " setup_db
if [ "$setup_db" = "s" ] || [ "$setup_db" = "S" ]; then
    print_info "Esperando a que MySQL esté listo..."
    sleep 15
    
    print_info "Ejecutando scripts de base de datos..."
    docker exec -i legal-mysql mysql -uroot -proot_password < database/schema.sql
    print_info "✓ Base de datos configurada"
fi

echo ""

# ============================================
# 9. CONFIGURAR KAFKA TOPICS
# ============================================

read -p "¿Deseas crear los topics de Kafka? (s/n): " setup_kafka
if [ "$setup_kafka" = "s" ] || [ "$setup_kafka" = "S" ]; then
    print_info "Creando topics de Kafka..."
    
    docker exec legal-kafka kafka-topics --create --topic case-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
    docker exec legal-kafka kafka-topics --create --topic notification-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
    docker exec legal-kafka kafka-topics --create --topic document-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
    docker exec legal-kafka kafka-topics --create --topic audit-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
    
    print_info "✓ Topics de Kafka creados"
fi

echo ""

# ============================================
# 10. RESUMEN Y PRÓXIMOS PASOS
# ============================================

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  CONFIGURACIÓN COMPLETADA${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""

print_info "Próximos pasos:"
echo ""
echo "1. Edita el archivo .env con tus credenciales reales:"
echo "   nano .env"
echo ""
echo "2. Para desarrollo local, inicia los servicios:"
echo "   docker-compose up -d"
echo ""
echo "3. Para iniciar el backend:"
echo "   cd backend"
echo "   ./start-services.sh"
echo ""
echo "4. Para iniciar el frontend:"
echo "   cd frontend/legal-app"
echo "   ng serve"
echo ""
echo "5. Accede a la aplicación en:"
echo "   http://localhost:4200"
echo ""
echo "6. Para desplegar en AWS:"
echo "   cd infrastructure/terraform"
echo "   terraform init"
echo "   terraform plan"
echo "   terraform apply"
echo ""

print_info "Documentación adicional:"
echo "  - Instalación completa: docs/INSTALLATION.md"
echo "  - Arquitectura: docs/architecture/ARCHITECTURE.md"
echo "  - API: docs/api/API.md"
echo "  - CI/CD: docs/guides/CICD.md"
echo ""

print_info "¡Setup completado exitosamente! 🎉"
echo ""
