# ÍNDICE COMPLETO DEL PROYECTO
# Sistema de Gestión Legal y Contable

## 📁 Estructura Completa de Archivos

```
legal-management-system/
│
├── README.md              ✅ CREADO                # Documentación principal
├── .env.example             ✅ CREADO             # Ejemplo de variables de entorno
├── .gitignore               ✅ CREADO             # Archivos ignorados por Git
├── docker-compose.yml      ✅ CREADO              # Orquestación de servicios Docker
│
├── .github/
│   └── workflows/
│       └── ci-cd.yml                     # Pipeline de CI/CD con GitHub Actions
│
├── backend/    ✅ CREADO                          # Backend - Microservicios Java
│   ├── pom.xml                          # POM principal (parent)
│   │
│   ├── config-server/                   # Servidor de configuración centralizada
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/legal/
│   │       │   └── ConfigServerApplication.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── eureka-server/                   # Service Discovery (Eureka)
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/legal/
│   │       │   └── EurekaServerApplication.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── api-gateway/                     # API Gateway (Spring Cloud Gateway)
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/legal/
│   │       │   ├── ApiGatewayApplication.java
│   │       │   ├── config/
│   │       │   │   ├── GatewayConfig.java
│   │       │   │   ├── CorsConfig.java
│   │       │   │   └── SecurityConfig.java
│   │       │   ├── filter/
│   │       │   │   ├── AuthenticationFilter.java
│   │       │   │   └── LoggingFilter.java
│   │       │   └── exception/
│   │       │       └── GlobalExceptionHandler.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── auth-service/                    # Servicio de Autenticación
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/legal/
│   │       │   ├── AuthServiceApplication.java
│   │       │   ├── controller/
│   │       │   │   ├── AuthController.java
│   │       │   │   └── OAuth2Controller.java
│   │       │   ├── service/
│   │       │   │   ├── AuthService.java
│   │       │   │   ├── JwtService.java
│   │       │   │   ├── OAuth2Service.java
│   │       │   │   └── DeviceRegistrationService.java
│   │       │   ├── repository/
│   │       │   │   ├── UserRepository.java
│   │       │   │   └── DeviceRegistrationRepository.java
│   │       │   ├── model/
│   │       │   │   ├── User.java
│   │       │   │   └── DeviceRegistration.java
│   │       │   ├── dto/
│   │       │   │   ├── LoginRequest.java
│   │       │   │   ├── LoginResponse.java
│   │       │   │   ├── RegisterRequest.java
│   │       │   │   └── OAuth2Request.java
│   │       │   ├── security/
│   │       │   │   ├── SecurityConfig.java
│   │       │   │   ├── JwtAuthenticationFilter.java
│   │       │   │   └── UserDetailsServiceImpl.java
│   │       │   ├── exception/
│   │       │   │   ├── AuthException.java
│   │       │   │   └── DeviceLimitExceededException.java
│   │       │   └── util/
│   │       │       └── PasswordUtil.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── user-service/                    # Servicio de Usuarios
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/legal/
│   │       │   ├── UserServiceApplication.java
│   │       │   ├── controller/
│   │       │   │   ├── UserController.java
│   │       │   │   └── RoleController.java
│   │       │   ├── service/
│   │       │   │   ├── UserService.java
│   │       │   │   └── RoleService.java
│   │       │   ├── repository/
│   │       │   │   ├── UserRepository.java
│   │       │   │   ├── RoleRepository.java
│   │       │   │   └── PermissionRepository.java
│   │       │   ├── model/
│   │       │   │   ├── User.java
│   │       │   │   ├── Role.java
│   │       │   │   ├── Permission.java
│   │       │   │   └── Category.java
│   │       │   ├── dto/
│   │       │   │   ├── UserDTO.java
│   │       │   │   ├── CreateUserRequest.java
│   │       │   │   └── UpdateUserRequest.java
│   │       │   └── exception/
│   │       │       └── UserNotFoundException.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── case-service/                    # Servicio de Casos
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/legal/
│   │       │   ├── CaseServiceApplication.java
│   │       │   ├── controller/
│   │       │   │   ├── CaseController.java
│   │       │   │   ├── VersionController.java
│   │       │   │   └── CommentController.java
│   │       │   ├── service/
│   │       │   │   ├── CaseService.java
│   │       │   │   ├── VersionService.java
│   │       │   │   ├── CollaboratorService.java
│   │       │   │   ├── QRCodeService.java
│   │       │   │   └── PDFGenerationService.java
│   │       │   ├── repository/
│   │       │   │   ├── CaseRepository.java
│   │       │   │   ├── CaseVersionRepository.java
│   │       │   │   ├── CollaboratorRepository.java
│   │       │   │   └── CommentRepository.java
│   │       │   ├── model/
│   │       │   │   ├── Case.java                  # ✅ CREADO
│   │       │   │   ├── CaseVersion.java
│   │       │   │   ├── CaseCollaborator.java
│   │       │   │   ├── VersionComment.java
│   │       │   │   └── CaseRelation.java
│   │       │   ├── dto/
│   │       │   │   ├── CaseDTO.java
│   │       │   │   ├── CreateCaseRequest.java
│   │       │   │   ├── UpdateCaseRequest.java
│   │       │   │   └── VersionDTO.java
│   │       │   ├── kafka/
│   │       │   │   ├── CaseEventProducer.java
│   │       │   │   └── CaseEventListener.java
│   │       │   └── exception/
│   │       │       └── CaseNotFoundException.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── notification-service/            # Servicio de Notificaciones
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/legal/
│   │       │   ├── NotificationServiceApplication.java
│   │       │   ├── controller/
│   │       │   │   └── NotificationController.java
│   │       │   ├── service/
│   │       │   │   ├── NotificationService.java
│   │       │   │   ├── EmailService.java
│   │       │   │   ├── SMSService.java
│   │       │   │   └── InactivityAlertService.java
│   │       │   ├── repository/
│   │       │   │   ├── NotificationRepository.java
│   │       │   │   ├── EmailNotificationRepository.java
│   │       │   │   └── SMSNotificationRepository.java
│   │       │   ├── model/
│   │       │   │   ├── Notification.java
│   │       │   │   ├── EmailNotification.java
│   │       │   │   └── SMSNotification.java
│   │       │   ├── kafka/
│   │       │   │   └── NotificationEventListener.java
│   │       │   ├── scheduler/
│   │       │   │   └── InactivityCheckScheduler.java
│   │       │   └── config/
│   │       │       ├── TwilioConfig.java
│   │       │       └── AmazonSESConfig.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   └── document-service/                # Servicio de Documentos
│       ├── pom.xml
│       ├── Dockerfile
│       └── src/main/
│           ├── java/com/legal/
│           │   ├── DocumentServiceApplication.java
│           │   ├── controller/
│           │   │   ├── DocumentController.java
│           │   │   └── S3BucketController.java
│           │   ├── service/
│           │   │   ├── DocumentService.java
│           │   │   ├── GoogleDriveService.java
│           │   │   ├── S3Service.java
│           │   │   └── FileValidationService.java
│           │   ├── repository/
│           │   │   ├── CaseDocumentRepository.java
│           │   │   ├── S3BucketRepository.java
│           │   │   └── S3FolderRepository.java
│           │   ├── model/
│           │   │   ├── CaseDocument.java
│           │   │   ├── S3Bucket.java
│           │   │   ├── S3Folder.java
│           │   │   └── S3File.java
│           │   ├── kafka/
│           │   │   └── DocumentEventListener.java
│           │   └── config/
│           │       ├── GoogleDriveConfig.java
│           │       └── AmazonS3Config.java
│           └── resources/
│               └── application.yml
│
├── frontend/                            # Frontend - Angular
│   └── legal-app/
│       ├── package.json                 # ✅ CREADO
│       ├── tsconfig.json
│       ├── angular.json
│       ├── Dockerfile
│       ├── nginx.conf
│       └── src/
│           ├── index.html
│           ├── main.ts
│           ├── styles.scss
│           ├── app/
│           │   ├── app.component.ts
│           │   ├── app.component.html
│           │   ├── app.component.scss
│           │   ├── app.routes.ts
│           │   │
│           │   ├── core/
│           │   │   ├── guards/
│           │   │   │   ├── auth.guard.ts
│           │   │   │   └── role.guard.ts
│           │   │   ├── interceptors/
│           │   │   │   ├── auth.interceptor.ts
│           │   │   │   ├── error.interceptor.ts
│           │   │   │   └── loading.interceptor.ts
│           │   │   ├── services/
│           │   │   │   ├── auth.service.ts
│           │   │   │   ├── case.service.ts
│           │   │   │   ├── user.service.ts
│           │   │   │   ├── notification.service.ts
│           │   │   │   └── websocket.service.ts
│           │   │   └── models/
│           │   │       ├── user.model.ts
│           │   │       ├── case.model.ts
│           │   │       └── notification.model.ts
│           │   │
│           │   ├── shared/
│           │   │   ├── components/
│           │   │   │   ├── header/
│           │   │   │   ├── sidebar/
│           │   │   │   ├── kanban-board/
│           │   │   │   ├── case-card/
│           │   │   │   ├── file-upload/
│           │   │   │   └── qr-code/
│           │   │   └── directives/
│           │   │       └── drag-drop.directive.ts
│           │   │
│           │   ├── features/
│           │   │   ├── auth/
│           │   │   │   ├── login/
│           │   │   │   ├── register/
│           │   │   │   └── oauth-callback/
│           │   │   ├── dashboard/
│           │   │   ├── cases/
│           │   │   │   ├── case-list/
│           │   │   │   ├── case-detail/
│           │   │   │   ├── case-form/
│           │   │   │   ├── version-list/
│           │   │   │   └── case-relations/
│           │   │   ├── users/
│           │   │   │   ├── user-list/
│           │   │   │   └── user-form/
│           │   │   ├── documents/
│           │   │   │   ├── document-repository/
│           │   │   │   └── s3-manager/
│           │   │   ├── notifications/
│           │   │   └── reports/
│           │   │
│           │   └── store/              # NgRx Store
│           │       ├── actions/
│           │       ├── reducers/
│           │       ├── effects/
│           │       └── selectors/
│           │
│           ├── assets/
│           │   ├── images/
│           │   ├── icons/
│           │   └── i18n/
│           │
│           └── environments/
│               ├── environment.ts
│               └── environment.prod.ts
│
├── database/                            # Base de Datos
│   ├── schema.sql                       # ✅ CREADO - Schema completo
│   ├── initial-data.sql                 # Datos iniciales
│   ├── migrations/                      # Migraciones Flyway/Liquibase
│   │   ├── V1__initial_schema.sql
│   │   ├── V2__add_indexes.sql
│   │   └── V3__add_triggers.sql
│   └── scripts/
│       ├── backup.sh
│       └── restore.sh
│
├── infrastructure/                      # Infraestructura
│   ├── kubernetes/                      # Manifiestos de Kubernetes
│   │   ├── namespaces/
│   │   │   ├── dev-namespace.yaml
│   │   │   ├── staging-namespace.yaml
│   │   │   └── prod-namespace.yaml
│   │   ├── config-server/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   └── configmap.yaml
│   │   ├── eureka-server/
│   │   │   ├── deployment.yaml
│   │   │   └── service.yaml
│   │   ├── api-gateway/
│   │   │   ├── deployment.yaml
│   │   │   ├── service.yaml
│   │   │   └── ingress.yaml
│   │   ├── auth-service/
│   │   ├── user-service/
│   │   ├── case-service/
│   │   ├── notification-service/
│   │   ├── document-service/
│   │   └── frontend/
│   │
│   ├── terraform/                       # Infraestructura como Código
│   │   ├── main.tf                      # ✅ CREADO
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── providers.tf
│   │   └── modules/
│   │       ├── vpc/
│   │       ├── rds/
│   │       ├── eks/
│   │       └── s3/
│   │
│   ├── docker/
│   │   └── Dockerfiles específicos
│   │
│   └── monitoring/
│       ├── prometheus.yml
│       └── grafana-dashboards/
│
├── docs/                                # Documentación
│   ├── INSTALLATION.md                  # ✅ CREADO - Guía completa de instalación
│   ├── api/
│   │   ├── API.md
│   │   ├── authentication.md
│   │   ├── cases.md
│   │   ├── users.md
│   │   └── postman-collection.json
│   ├── architecture/
│   │   ├── ARCHITECTURE.md
│   │   ├── microservices.md
│   │   ├── database-design.md
│   │   └── diagrams/
│   │       ├── system-architecture.png
│   │       ├── database-er-diagram.png
│   │       └── deployment-diagram.png
│   └── guides/
│       ├── CICD.md
│       ├── AWS_DEPLOYMENT.md
│       ├── BEST_PRACTICES.md
│       ├── SECURITY.md
│       └── TROUBLESHOOTING.md
│
└── scripts/                             # Scripts de Utilidad
    ├── setup.sh                         # ✅ CREADO - Setup inicial
    ├── deployment/
    │   ├── deploy-dev.sh
    │   ├── deploy-staging.sh
    │   └── deploy-prod.sh
    ├── setup/
    │   ├── install-dependencies.sh
    │   ├── configure-aws.sh
    │   └── setup-database.sh
    └── utilities/
        ├── backup-database.sh
        ├── restore-database.sh
        └── generate-migration.sh
```

## 📊 Archivos Creados en Este Setup

### ✅ Archivos Principales Creados:

1. **README.md** - Documentación principal del proyecto
2. **database/schema.sql** - Schema completo de la base de datos
3. **backend/pom.xml** - POM principal para todos los microservicios
4. **backend/case-service/.../Case.java** - Modelo de entidad Case
5. **docker-compose.yml** - Orquestación completa de servicios
6. **infrastructure/terraform/main.tf** - Configuración completa de AWS
7. **docs/INSTALLATION.md** - Guía completa de instalación paso a paso
8. **scripts/setup.sh** - Script automatizado de configuración inicial
9. **.github/workflows/ci-cd.yml** - Pipeline completo de CI/CD
10. **frontend/legal-app/package.json** - Configuración del proyecto Angular

## 🎯 Próximos Pasos para Implementación Completa

### 1. Microservicios Backend (Java/Spring Boot)
- [ ] Completar todos los controllers, services y repositories
- [ ] Implementar DTOs y mappers (MapStruct)
- [ ] Configurar Spring Security y JWT
- [ ] Implementar Kafka producers y consumers
- [ ] Agregar tests unitarios y de integración

### 2. Frontend Angular
- [ ] Generar proyecto Angular con CLI
- [ ] Implementar componentes de UI
- [ ] Configurar NgRx para state management
- [ ] Implementar guards y interceptors
- [ ] Conectar con backend via servicios
- [ ] Agregar tests E2E con Cypress

### 3. Configuración de AWS
- [ ] Ejecutar Terraform para crear infraestructura
- [ ] Configurar RDS y migraciones de BD
- [ ] Configurar S3 buckets y políticas
- [ ] Configurar SES para emails
- [ ] Configurar SNS para SMS (Twilio)
- [ ] Configurar EKS cluster

### 4. Implementación de Google Drive
- [ ] Configurar credenciales de servicio
- [ ] Implementar GoogleDriveService
- [ ] Crear carpetas automáticas por caso
- [ ] Implementar sincronización de documentos

### 5. CI/CD
- [ ] Configurar GitHub Actions
- [ ] Configurar ECR repositories
- [ ] Implementar pipeline de despliegue
- [ ] Configurar monitoreo con CloudWatch

## 📚 Documentación Adicional Necesaria

- [ ] API Documentation (OpenAPI/Swagger)
- [ ] Diagramas de Arquitectura
- [ ] Manual de Usuario
- [ ] Guía de Desarrollo
- [ ] Políticas de Seguridad
- [ ] Plan de Disaster Recovery

## 🔧 Herramientas y Tecnologías Utilizadas

- **Backend**: Java 17, Spring Boot 3, Spring Cloud, JPA/Hibernate
- **Frontend**: Angular 17, TypeScript, Angular Material, NgRx
- **Base de Datos**: MySQL 8.0
- **Cache**: Redis
- **Message Broker**: Apache Kafka
- **Almacenamiento**: AWS S3, Google Drive API
- **Contenedores**: Docker, Kubernetes (EKS)
- **IaC**: Terraform
- **CI/CD**: GitHub Actions
- **Monitoreo**: Prometheus, Grafana
- **Notificaciones**: AWS SES, Twilio

---

**Nota**: Este índice representa la estructura completa del proyecto. Los archivos marcados con ✅ han sido creados en este setup inicial. Los demás archivos deben ser completados siguiendo las especificaciones y mejores prácticas documentadas.
