# Guía de Instalación Completa - Sistema de Gestión Legal y Contable

## 📋 Tabla de Contenidos

1. [Requisitos Previos](#requisitos-previos)
2. [Instalación de Herramientas](#instalación-de-herramientas)
3. [Configuración de AWS](#configuración-de-aws)
4. [Configuración de Google Drive](#configuración-de-google-drive)
5. [Configuración de Base de Datos](#configuración-de-base-de-datos)
6. [Instalación de Kafka](#instalación-de-kafka)
7. [Configuración del Backend](#configuración-del-backend)
8. [Configuración del Frontend](#configuración-del-frontend)
9. [Despliegue con Kubernetes](#despliegue-con-kubernetes)
10. [Verificación](#verificación)

---

## 1. Requisitos Previos

### Hardware Mínimo
- **CPU**: 4 cores
- **RAM**: 8 GB (16 GB recomendado)
- **Disco**: 50 GB libres

### Cuentas Necesarias
- ✅ Cuenta de AWS
- ✅ Cuenta de Google Cloud (para Google Drive API)
- ✅ Cuenta de Twilio (para SMS)
- ✅ Dominio propio (opcional pero recomendado)

---

## 2. Instalación de Herramientas

### 2.1 Java 17
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk -y

# Verificar instalación
java -version

# Configurar JAVA_HOME
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

### 2.2 Maven
```bash
# Descargar Maven 3.9.x
cd /opt
sudo wget https://dlcdn.apache.org/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.tar.gz
sudo tar xzf apache-maven-3.9.5-bin.tar.gz
sudo ln -s apache-maven-3.9.5 maven

# Configurar variables de entorno
echo 'export M2_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$M2_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc

# Verificar
mvn -version
```

### 2.3 Node.js y npm
```bash
# Instalar NVM (Node Version Manager)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash
source ~/.bashrc

# Instalar Node.js 18 LTS
nvm install 18
nvm use 18

# Verificar
node -v
npm -v
```

### 2.4 Angular CLI
```bash
npm install -g @angular/cli@17

# Verificar
ng version
```

### 2.5 Docker y Docker Compose
```bash
# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Agregar usuario al grupo docker
sudo usermod -aG docker $USER
newgrp docker

# Instalar Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Verificar
docker --version
docker-compose --version
```

### 2.6 kubectl (Kubernetes CLI)
```bash
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Verificar
kubectl version --client
```

### 2.7 AWS CLI
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Verificar
aws --version
```

### 2.8 Terraform (Infraestructura como Código)
```bash
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install terraform -y

# Verificar
terraform -version
```

### 2.9 Git
```bash
sudo apt install git -y
git config --global user.name "Tu Nombre"
git config --global user.email "tu@email.com"
```

---

## 3. Configuración de AWS

### 3.1 Configurar Credenciales de AWS
```bash
aws configure
```
Ingresar:
- AWS Access Key ID
- AWS Secret Access Key
- Default region: `us-east-1`
- Default output format: `json`

### 3.2 Crear Usuario IAM para la Aplicación
```bash
# Desde AWS Console:
# 1. IAM > Users > Create User
# 2. Nombre: legal-system-app
# 3. Permisos:
#    - AmazonS3FullAccess
#    - AmazonRDSFullAccess
#    - AmazonSESFullAccess
#    - AmazonSNSFullAccess
#    - AWSLambdaFullAccess
# 4. Generar Access Keys
# 5. Guardar las credenciales
```

### 3.3 Crear Bucket S3
```bash
# Bucket para documentos privados del admin
aws s3 mb s3://legal-system-admin-private --region us-east-1

# Configurar CORS
cat > cors.json <<EOF
{
    "CORSRules": [
        {
            "AllowedHeaders": ["*"],
            "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
            "AllowedOrigins": ["*"],
            "ExposeHeaders": []
        }
    ]
}
EOF

aws s3api put-bucket-cors --bucket legal-system-admin-private --cors-configuration file://cors.json
```

### 3.4 Configurar RDS (MySQL)
```bash
# Desde AWS Console o con Terraform (ver sección de Terraform)
# Configuración recomendada:
# - Engine: MySQL 8.0
# - Instance class: db.t3.medium (producción) o db.t3.micro (desarrollo)
# - Storage: 20 GB SSD
# - Multi-AZ: Yes (producción)
# - VPC: Default VPC
# - Public access: No
# - Security Group: Permitir puerto 3306 desde las instancias EC2/EKS
```

### 3.5 Configurar SES para Email
```bash
# Verificar un dominio o email
aws ses verify-email-identity --email-address noreply@tudominio.com --region us-east-1

# Solicitar salir de Sandbox (producción)
# AWS Console > SES > Account Dashboard > Request Production Access
```

### 3.6 Configurar SNS para Notificaciones
```bash
# Crear un topic para notificaciones
aws sns create-topic --name legal-system-notifications --region us-east-1

# Guardar el ARN que se retorna
```

---

## 4. Configuración de Google Drive API

### 4.1 Crear Proyecto en Google Cloud Console
1. Ir a: https://console.cloud.google.com/
2. Crear nuevo proyecto: "Legal Management System"
3. Habilitar APIs:
   - Google Drive API
   - Google OAuth2 API

### 4.2 Configurar OAuth2
```
1. APIs & Services > Credentials
2. Create Credentials > OAuth 2.0 Client ID
3. Application type: Web application
4. Authorized redirect URIs:
   - http://localhost:4200/auth/google/callback
   - https://tudominio.com/auth/google/callback
5. Descargar JSON de credenciales
6. Guardar como: google-credentials.json
```

### 4.3 Crear Cuenta de Servicio para Backend
```
1. IAM & Admin > Service Accounts
2. Create Service Account
3. Name: legal-system-drive
4. Role: Editor
5. Create Key (JSON)
6. Guardar como: service-account.json
```

---

## 5. Configuración de Base de Datos

### 5.1 Conectarse a RDS
```bash
# Obtener endpoint de RDS
aws rds describe-db-instances --db-instance-identifier legal-db --query 'DBInstances[0].Endpoint.Address'

# Conectarse (reemplazar con tu endpoint)
mysql -h legal-db.xxxxxxxxx.us-east-1.rds.amazonaws.com -P 3306 -u admin -p
```

### 5.2 Ejecutar Scripts de Base de Datos
```bash
# Desde tu máquina local
mysql -h <RDS_ENDPOINT> -u admin -p < database/schema.sql
mysql -h <RDS_ENDPOINT> -u admin -p < database/initial-data.sql
```

### 5.3 Crear Usuario de Aplicación
```sql
CREATE USER 'legal_app'@'%' IDENTIFIED BY 'password_seguro_aqui';
GRANT ALL PRIVILEGES ON legal_management_db.* TO 'legal_app'@'%';
FLUSH PRIVILEGES;
```

---

## 6. Instalación de Kafka

### 6.1 Opción 1: Docker Compose (Desarrollo)
```bash
cd infrastructure/docker

# Crear archivo docker-compose.yml para Kafka
cat > docker-compose-kafka.yml <<EOF
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
EOF

docker-compose -f docker-compose-kafka.yml up -d
```

### 6.2 Opción 2: Amazon MSK (Producción)
```bash
# Ver guía de AWS MSK en: docs/guides/AWS_MSK_SETUP.md
```

### 6.3 Crear Topics de Kafka
```bash
# Conectarse al contenedor de Kafka
docker exec -it kafka bash

# Crear topics necesarios
kafka-topics --create --topic case-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic notification-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic document-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics --create --topic audit-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Verificar topics
kafka-topics --list --bootstrap-server localhost:9092
```

---

## 7. Configuración del Backend

### 7.1 Clonar o Copiar el Proyecto
```bash
cd /opt
git clone <repository-url> legal-management-system
cd legal-management-system/backend
```

### 7.2 Configurar Variables de Entorno
```bash
# Crear archivo .env en cada microservicio o centralizado
cp .env.example .env

# Editar .env con tus credenciales
nano .env
```

Contenido del `.env`:
```env
# Database
DB_HOST=legal-db.xxxxxxxxx.us-east-1.rds.amazonaws.com
DB_PORT=3306
DB_NAME=legal_management_db
DB_USER=legal_app
DB_PASSWORD=password_seguro_aqui

# JWT
JWT_SECRET=tu_secreto_jwt_muy_seguro_de_al_menos_256_bits
JWT_EXPIRATION=86400000

# AWS
AWS_ACCESS_KEY_ID=AKIAXXXXXXXXXXXXXXXX
AWS_SECRET_ACCESS_KEY=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
AWS_REGION=us-east-1
AWS_S3_BUCKET=legal-system-admin-private
AWS_SES_FROM_EMAIL=noreply@tudominio.com
AWS_SNS_TOPIC_ARN=arn:aws:sns:us-east-1:xxxxxxxxxxxx:legal-system-notifications

# Google Drive
GOOGLE_CLIENT_ID=xxxxxxxx-xxxxxxxx.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-xxxxxxxxxxxxxxxxxxxxxxxx
GOOGLE_REDIRECT_URI=http://localhost:4200/auth/google/callback
GOOGLE_SERVICE_ACCOUNT_PATH=/config/service-account.json

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Twilio
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_PHONE_NUMBER=+1234567890

# Redis (opcional para cache)
REDIS_HOST=localhost
REDIS_PORT=6379

# Microservices ports
CONFIG_SERVER_PORT=8888
EUREKA_SERVER_PORT=8761
API_GATEWAY_PORT=8080
AUTH_SERVICE_PORT=8081
USER_SERVICE_PORT=8082
CASE_SERVICE_PORT=8083
NOTIFICATION_SERVICE_PORT=8084
DOCUMENT_SERVICE_PORT=8085
```

### 7.3 Compilar el Backend
```bash
cd /opt/legal-management-system/backend

# Compilar todo el proyecto
mvn clean install -DskipTests

# O compilar servicio por servicio
cd config-server && mvn clean package
cd ../eureka-server && mvn clean package
cd ../api-gateway && mvn clean package
# ... etc
```

### 7.4 Iniciar Microservicios (Orden Importante)
```bash
# 1. Config Server (primero siempre)
cd config-server
java -jar target/config-server-1.0.0.jar &

# Esperar 30 segundos

# 2. Eureka Server
cd ../eureka-server
java -jar target/eureka-server-1.0.0.jar &

# Esperar 30 segundos

# 3. Servicios de negocio (pueden iniciarse en paralelo)
cd ../auth-service
java -jar target/auth-service-1.0.0.jar &

cd ../user-service
java -jar target/user-service-1.0.0.jar &

cd ../case-service
java -jar target/case-service-1.0.0.jar &

cd ../notification-service
java -jar target/notification-service-1.0.0.jar &

cd ../document-service
java -jar target/document-service-1.0.0.jar &

# 4. API Gateway (último)
cd ../api-gateway
java -jar target/api-gateway-1.0.0.jar &
```

### 7.5 Verificar Microservicios
```bash
# Verificar Eureka Dashboard
curl http://eureka-server:8761

# Verificar que todos los servicios estén registrados
curl http://eureka-server:8761/eureka/apps | grep "<app>"
```

---

## 8. Configuración del Frontend

### 8.1 Instalar Dependencias
```bash
cd /opt/legal-management-system/frontend/legal-app
npm install
```

### 8.2 Configurar Entornos
```bash
# Editar environment.ts para desarrollo
nano src/environments/environment.ts
```

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  googleClientId: 'tu-google-client-id',
  microsoftClientId: 'tu-microsoft-client-id'
};
```

```bash
# Editar environment.prod.ts para producción
nano src/environments/environment.prod.ts
```

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.tudominio.com/api',
  googleClientId: 'tu-google-client-id',
  microsoftClientId: 'tu-microsoft-client-id'
};
```

### 8.3 Iniciar en Desarrollo
```bash
ng serve --host 0.0.0.0 --port 4200
```

### 8.4 Compilar para Producción
```bash
ng build --configuration production

# Los archivos estarán en: dist/legal-app
```

---

## 9. Despliegue con Kubernetes

### 9.1 Crear Cluster EKS en AWS
```bash
# Instalar eksctl
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# Crear cluster
eksctl create cluster \
  --name legal-system-cluster \
  --region us-east-1 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 3 \
  --nodes-min 2 \
  --nodes-max 4 \
  --managed

# Verificar
kubectl get nodes
```

### 9.2 Configurar kubectl para EKS
```bash
aws eks update-kubeconfig --region us-east-1 --name legal-system-cluster
```

### 9.3 Crear Namespaces
```bash
kubectl create namespace legal-system-dev
kubectl create namespace legal-system-prod
```

### 9.4 Crear Secrets en Kubernetes
```bash
# Secret para base de datos
kubectl create secret generic db-credentials \
  --from-literal=host=$DB_HOST \
  --from-literal=port=$DB_PORT \
  --from-literal=database=$DB_NAME \
  --from-literal=username=$DB_USER \
  --from-literal=password=$DB_PASSWORD \
  -n legal-system-prod

# Secret para AWS
kubectl create secret generic aws-credentials \
  --from-literal=access-key=$AWS_ACCESS_KEY_ID \
  --from-literal=secret-key=$AWS_SECRET_ACCESS_KEY \
  -n legal-system-prod

# Secret para JWT
kubectl create secret generic jwt-secret \
  --from-literal=secret=$JWT_SECRET \
  -n legal-system-prod
```

### 9.5 Desplegar Aplicaciones
```bash
cd /opt/legal-management-system/infrastructure/kubernetes

# Aplicar configuraciones
kubectl apply -f config-server/ -n legal-system-prod
kubectl apply -f eureka-server/ -n legal-system-prod
kubectl apply -f api-gateway/ -n legal-system-prod
kubectl apply -f auth-service/ -n legal-system-prod
kubectl apply -f user-service/ -n legal-system-prod
kubectl apply -f case-service/ -n legal-system-prod
kubectl apply -f notification-service/ -n legal-system-prod
kubectl apply -f document-service/ -n legal-system-prod

# Verificar deployments
kubectl get deployments -n legal-system-prod
kubectl get pods -n legal-system-prod
kubectl get services -n legal-system-prod
```

### 9.6 Exponer API Gateway con LoadBalancer
```bash
kubectl expose deployment api-gateway \
  --type=LoadBalancer \
  --name=api-gateway-lb \
  --port=80 \
  --target-port=8080 \
  -n legal-system-prod

# Obtener URL pública
kubectl get service api-gateway-lb -n legal-system-prod
```

---

## 10. Verificación

### 10.1 Verificar Backend
```bash
# Health check de cada servicio
curl http://<api-gateway-url>/actuator/health
curl http://<api-gateway-url>/auth-service/actuator/health
curl http://<api-gateway-url>/user-service/actuator/health
curl http://<api-gateway-url>/case-service/actuator/health
```

### 10.2 Verificar Base de Datos
```bash
mysql -h <RDS_ENDPOINT> -u legal_app -p

# En MySQL:
USE legal_management_db;
SHOW TABLES;
SELECT COUNT(*) FROM roles;
SELECT COUNT(*) FROM categories;
```

### 10.3 Verificar Kafka
```bash
# Listar topics
kafka-topics --list --bootstrap-server localhost:9092

# Ver mensajes (ejemplo)
kafka-console-consumer --bootstrap-server localhost:9092 --topic case-events --from-beginning
```

### 10.4 Prueba End-to-End
1. Abrir navegador en `http://localhost:4200` o URL de producción
2. Registrarse con Google o email
3. Verificar email de bienvenida
4. Login con cuenta creada
5. Crear un caso de prueba
6. Verificar que se envíe email con PDF
7. Crear una versión nueva
8. Verificar notificaciones

### 10.5 Verificar Logs
```bash
# Logs de Kubernetes
kubectl logs -f <pod-name> -n legal-system-prod

# Logs locales (desarrollo)
tail -f backend/case-service/logs/application.log
```

---

## 11. Troubleshooting

### Problema: Microservicios no se registran en Eureka
**Solución:**
```bash
# Verificar que Config Server esté activo
curl http://localhost:8888/actuator/health

# Verificar que Eureka esté activo
curl http://eureka-server:8761/eureka/apps

# Revisar logs
kubectl logs -f eureka-server-pod -n legal-system-prod
```

### Problema: Error de conexión a Base de Datos
**Solución:**
```bash
# Verificar Security Group de RDS
aws ec2 describe-security-groups --group-ids <sg-id>

# Verificar conectividad
telnet <rds-endpoint> 3306

# Verificar credenciales en secrets
kubectl describe secret db-credentials -n legal-system-prod
```

### Problema: Kafka no recibe mensajes
**Solución:**
```bash
# Verificar topics
kafka-topics --list --bootstrap-server localhost:9092

# Verificar consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Ver offset de consumer
kafka-consumer-groups --bootstrap-server localhost:9092 --group notification-service --describe
```

---

## 12. Próximos Pasos

1. ✅ Configurar CI/CD (ver `docs/guides/CICD.md`)
2. ✅ Configurar monitoreo (Prometheus + Grafana)
3. ✅ Configurar alertas (CloudWatch)
4. ✅ Implementar backup automático de BD
5. ✅ Configurar WAF para seguridad
6. ✅ Implementar autoscaling en EKS
7. ✅ Configurar dominio y SSL/TLS

---

## 📚 Recursos Adicionales

- [Arquitectura del Sistema](../architecture/ARCHITECTURE.md)
- [API Documentation](../api/API.md)
- [Guía de CI/CD](CICD.md)
- [Guía de AWS](AWS_DEPLOYMENT.md)
- [Mejores Prácticas](BEST_PRACTICES.md)

---

## 🆘 Soporte

Para soporte técnico:
- Email: support@tudominio.com
- Slack: #legal-system-support
- GitHub Issues: [repository-url]/issues
