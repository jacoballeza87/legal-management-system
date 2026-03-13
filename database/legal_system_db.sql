-- =============================================================================
-- LEGAL CASE SYSTEM - Script Completo de Base de Datos
-- Base de Datos: legal-system-db
-- Motor: MySQL 8.0
-- Encoding: utf8mb4
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CONFIGURACIÓN INICIAL
-- -----------------------------------------------------------------------------
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------------------------------
-- CREAR Y SELECCIONAR BASE DE DATOS
-- -----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `legal_system_db`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `legal_system_db`;

-- -----------------------------------------------------------------------------
-- CREAR USUARIO DE APLICACIÓN
-- Separado del usuario admin, con permisos mínimos necesarios
-- -----------------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'app_user'@'%' IDENTIFIED BY 'Ant0n14BM87';
CREATE USER IF NOT EXISTS 'app_user'@'localhost' IDENTIFIED BY 'Ant0n14BM87';

GRANT SELECT, INSERT, UPDATE, DELETE ON `legal_system_db`.* TO 'app_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `legal_system_db`.* TO 'app_user'@'localhost';

-- Usuario de solo lectura para reportes / monitoreo
CREATE USER IF NOT EXISTS 'readonly_user'@'%' IDENTIFIED BY 'R3adOnly_2024!';
GRANT SELECT ON `legal_system_db`.* TO 'readonly_user'@'%';

FLUSH PRIVILEGES;

-- =============================================================================
-- SECCIÓN 1: CATÁLOGOS Y TABLAS DE REFERENCIA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: categories — Contable / Jurídico (extensible)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `categories` (
  `id`          TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(30)  NOT NULL,
  `name`        VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) NULL,
  `is_active`   TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_category_code` (`code`)
) ENGINE=InnoDB COMMENT='Categorías del sistema: ACCOUNTING, LEGAL, etc.';

INSERT INTO `categories` (`code`, `name`, `description`) VALUES
  ('ACCOUNTING', 'Contable',  'Casos y usuarios del área contable'),
  ('LEGAL',      'Jurídico',  'Casos y usuarios del área jurídica / abogados')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- -----------------------------------------------------------------------------
-- Tabla: roles — Roles del sistema
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `roles` (
  `id`          TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(40)  NOT NULL,
  `name`        VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) NULL,
  `category_id` TINYINT UNSIGNED NULL COMMENT 'NULL = rol global (sys_admin)',
  `permissions` JSON         NULL    COMMENT 'Permisos granulares en JSON',
  `is_active`   TINYINT(1)   NOT NULL DEFAULT 1,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_role_code` (`code`),
  CONSTRAINT `fk_role_category`
    FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Roles: PENDING, SYS_ADMIN, ADMIN, SUPERVISOR, LAWYER, ACCOUNTANT, COLLABORATOR';

INSERT INTO `roles` (`code`, `name`, `description`, `category_id`, `permissions`) VALUES
  ('PENDING',       'Pendiente',             'Recién registrado, sin rol asignado',     NULL, NULL),
  ('SYS_ADMIN',     'Administrador Sistema', 'Acceso total al sistema',                 NULL,
      '{"cases":{"create":true,"read":true,"update":true,"delete":true,"reasign":true},"users":{"manage":true},"buckets":{"full_access":true},"versions":{"delete":true},"comments":{"delete":true}}'),
  ('ADMIN',         'Administrador',         'Administra casos y usuarios de su área',  NULL,
      '{"cases":{"create":true,"read":true,"update":true,"delete":false,"reasign":true},"users":{"manage":true},"buckets":{"upload":true}}'),
  ('SUPERVISOR',    'Supervisor',            'Supervisa por categoría',                 NULL,
      '{"cases":{"create":true,"read":true,"update":true,"delete":false},"users":{"assign_roles":true},"buckets":{"read":true}}'),
  ('LAWYER',        'Abogado / Jurídico',    'Gestiona casos jurídicos',                2,
      '{"cases":{"create":true,"read":true,"update":true,"delete":false}}'),
  ('ACCOUNTANT',    'Contador',              'Gestiona casos contables',                1,
      '{"cases":{"create":true,"read":true,"update":true,"delete":false}}'),
  ('COLLABORATOR',  'Colaborador',           'Colabora en casos asignados',             NULL,
      '{"cases":{"read":true,"update":true,"comment":true}}')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- -----------------------------------------------------------------------------
-- Tabla: case_statuses — Catálogo de estados del caso
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `case_statuses` (
  `id`              TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `code`            VARCHAR(30)  NOT NULL,
  `name`            VARCHAR(80)  NOT NULL,
  `description`     VARCHAR(255) NULL,
  `allows_reopen`   TINYINT(1)   NOT NULL DEFAULT 1  COMMENT '0 = solo certificado no reabre',
  `is_final`        TINYINT(1)   NOT NULL DEFAULT 0  COMMENT '1 = estado terminal',
  `display_order`   TINYINT      NOT NULL DEFAULT 99,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_status_code` (`code`)
) ENGINE=InnoDB COMMENT='Estados del ciclo de vida de un caso';

INSERT INTO `case_statuses` (`code`, `name`, `description`, `allows_reopen`, `is_final`, `display_order`) VALUES
  ('NOT_CREATED',      'No Creado',             'Estado previo a la creación',      1, 0, 1),
  ('CREATED',          'Creado',                'Caso registrado en el sistema',     1, 0, 2),
  ('ASSIGNED',         'Asignado',              'Asignado a un usuario',             1, 0, 3),
  ('UPDATED',          'Actualizado',           'Nueva versión publicada',           1, 0, 4),
  ('IN_APPROVAL',      'En Aprobación',         'Pendiente de aprobación',           1, 0, 5),
  ('CLOSED',           'Cerrado',               'Cerrado (no implica terminado)',     1, 0, 6),
  ('COMPLETED',        'Completado',            'Completado con resultado',          1, 0, 7),
  ('FINISHED_FAIL',    'Terminado sin Éxito',   'Concluido desfavorablemente',       1, 1, 8),
  ('FINISHED_SUCCESS', 'Terminado con Éxito',   'Concluido favorablemente',          1, 1, 9),
  ('REOPENED',         'Reabierto',             'Reabierto después de cerrar',       1, 0, 10),
  ('CERTIFIED',        'Certificado',           'Certificado — no puede reabrirse',  0, 1, 11)
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- -----------------------------------------------------------------------------
-- Tabla: authorities — Autoridades / Juzgados
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `authorities` (
  `id`          INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(255) NOT NULL,
  `type`        ENUM('FEDERAL','LOCAL','TRIBUNAL','LABOR','CIVIL','MERCANTIL','ADMINISTRATIVE','OTHER') NOT NULL DEFAULT 'OTHER',
  `state`       VARCHAR(100) NULL,
  `city`        VARCHAR(100) NULL,
  `address`     TEXT         NULL,
  `phone`       VARCHAR(20)  NULL,
  `email`       VARCHAR(150) NULL,
  `notes`       TEXT         NULL,
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FULLTEXT KEY `ft_authority_name` (`name`, `address`)
) ENGINE=InnoDB COMMENT='Catálogo de autoridades, juzgados y tribunales';

-- =============================================================================
-- SECCIÓN 2: USUARIOS Y AUTENTICACIÓN
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: users
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id`                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `uuid`                  CHAR(36)         NOT NULL  COMMENT 'UUID público expuesto en APIs',
  `email`                 VARCHAR(180)     NOT NULL,
  `email_verified`        TINYINT(1)       NOT NULL DEFAULT 0,
  `email_alt`             VARCHAR(180)     NULL      COMMENT 'Email alternativo para notificaciones',
  `password_hash`         VARCHAR(255)     NULL      COMMENT 'NULL si usa OAuth',
  `first_name`            VARCHAR(100)     NOT NULL,
  `last_name`             VARCHAR(100)     NOT NULL,
  `phone`                 VARCHAR(25)      NOT NULL,
  `photo_url`             VARCHAR(500)     NULL,
  `role_id`               TINYINT UNSIGNED NOT NULL,
  `category_id`           TINYINT UNSIGNED NULL,
  `status`                ENUM('PENDING','ACTIVE','INACTIVE','SUSPENDED','LOCKED') NOT NULL DEFAULT 'PENDING',
  `oauth_provider`        ENUM('LOCAL','GOOGLE','MICROSOFT') NOT NULL DEFAULT 'LOCAL',
  `oauth_provider_id`     VARCHAR(255)     NULL,
  `temp_password`         VARCHAR(255)     NULL      COMMENT 'Contraseña temporal para OAuth',
  `temp_password_expires` DATETIME         NULL,
  `last_login_at`         DATETIME         NULL,
  `failed_login_attempts` TINYINT UNSIGNED NOT NULL DEFAULT 0,
  `locked_until`          DATETIME         NULL,
  `created_at`            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`            DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`            DATETIME         NULL      COMMENT 'Soft delete',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_uuid`  (`uuid`),
  UNIQUE KEY `uq_user_email` (`email`),
  KEY `idx_user_role`     (`role_id`),
  KEY `idx_user_category` (`category_id`),
  KEY `idx_user_status`   (`status`),
  KEY `idx_user_oauth`    (`oauth_provider`, `oauth_provider_id`),
  CONSTRAINT `fk_user_role`
    FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `fk_user_category`
    FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB COMMENT='Usuarios del sistema';

-- Trigger: generar UUID automáticamente al insertar
DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `trg_users_before_insert`
BEFORE INSERT ON `users`
FOR EACH ROW
BEGIN
  IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
    SET NEW.uuid = UUID();
  END IF;
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Tabla: user_devices — Dispositivos autorizados por usuario
-- (máx 2 para SYS_ADMIN, sin límite para otros)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_devices` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `device_id`   VARCHAR(255)    NOT NULL  COMMENT 'Fingerprint del dispositivo',
  `device_name` VARCHAR(150)    NULL      COMMENT 'Ej: Chrome en Windows 11',
  `ip_address`  VARCHAR(45)     NULL      COMMENT 'IPv4 o IPv6',
  `user_agent`  VARCHAR(512)    NULL,
  `is_trusted`  TINYINT(1)      NOT NULL DEFAULT 1,
  `last_used_at` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_device` (`user_id`, `device_id`),
  KEY `idx_device_user` (`user_id`),
  CONSTRAINT `fk_device_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Dispositivos autorizados por usuario';

-- -----------------------------------------------------------------------------
-- Tabla: refresh_tokens — JWT refresh tokens
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `token_hash`  VARCHAR(255)    NOT NULL,
  `device_id`   VARCHAR(255)    NULL,
  `expires_at`  DATETIME        NOT NULL,
  `revoked_at`  DATETIME        NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_token_hash` (`token_hash`),
  KEY `idx_rt_user`    (`user_id`),
  KEY `idx_rt_expires` (`expires_at`),
  CONSTRAINT `fk_rt_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='JWT refresh tokens activos';

-- =============================================================================
-- SECCIÓN 3: CLIENTES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: clients — Clientes que solicitan casos
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `clients` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid`        CHAR(36)        NOT NULL,
  `full_name`   VARCHAR(200)    NOT NULL,
  `company`     VARCHAR(200)    NULL,
  `email`       VARCHAR(180)    NULL,
  `phone`       VARCHAR(25)     NOT NULL,
  `address`     TEXT            NOT NULL,
  `city`        VARCHAR(100)    NULL,
  `state`       VARCHAR(100)    NULL,
  `notes`       TEXT            NULL,
  `created_by`  BIGINT UNSIGNED NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_client_uuid` (`uuid`),
  KEY `idx_client_email` (`email`),
  FULLTEXT KEY `ft_client_name` (`full_name`, `company`)
) ENGINE=InnoDB COMMENT='Clientes que solicitan casos legales/contables';

DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `trg_clients_before_insert`
BEFORE INSERT ON `clients`
FOR EACH ROW
BEGIN
  IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
    SET NEW.uuid = UUID();
  END IF;
END$$
DELIMITER ;

-- =============================================================================
-- SECCIÓN 4: CASOS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: cases — Tabla principal de casos
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cases` (
  `id`                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `uuid`                CHAR(36)         NOT NULL  COMMENT 'ID público',
  `case_number`         VARCHAR(50)      NOT NULL  COMMENT 'Número de expediente (único)',
  `title`               VARCHAR(300)     NOT NULL,
  `description`         TEXT             NULL,
  `category_id`         TINYINT UNSIGNED NOT NULL,
  `status_id`           TINYINT UNSIGNED NOT NULL,
  `client_id`           BIGINT UNSIGNED  NOT NULL,
  `owner_id`            BIGINT UNSIGNED  NOT NULL  COMMENT 'Dueño / responsable principal',
  `authority_id`        INT UNSIGNED     NULL      COMMENT 'Juzgado o autoridad',
  `court_number`        VARCHAR(100)     NULL      COMMENT 'Número de juzgado',
  `counterpart`         VARCHAR(300)     NULL      COMMENT 'Vs. quién / contraparte',
  `qr_code_url`         VARCHAR(500)     NULL,
  `qr_code_s3_key`      VARCHAR(500)     NULL,
  `estimated_end_date`  DATE             NULL,
  `closed_at`           DATETIME         NULL,
  `certified_at`        DATETIME         NULL,
  `client_collab_enabled` TINYINT(1)     NOT NULL DEFAULT 0,
  `client_collab_reason`  TEXT           NULL,
  `parent_case_id`      BIGINT UNSIGNED  NULL      COMMENT 'Caso padre (solo admin asigna)',
  `created_by`          BIGINT UNSIGNED  NULL,
  `created_at`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at`          DATETIME         NULL      COMMENT 'Soft delete, solo SYS_ADMIN',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_case_uuid`   (`uuid`),
  UNIQUE KEY `uq_case_number` (`case_number`),
  KEY `idx_case_category`    (`category_id`),
  KEY `idx_case_status`      (`status_id`),
  KEY `idx_case_client`      (`client_id`),
  KEY `idx_case_owner`       (`owner_id`),
  KEY `idx_case_authority`   (`authority_id`),
  KEY `idx_case_parent`      (`parent_case_id`),
  KEY `idx_case_created_at`  (`created_at`),
  CONSTRAINT `fk_case_category`
    FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT `fk_case_status`
    FOREIGN KEY (`status_id`) REFERENCES `case_statuses` (`id`),
  CONSTRAINT `fk_case_client`
    FOREIGN KEY (`client_id`) REFERENCES `clients` (`id`),
  CONSTRAINT `fk_case_owner`
    FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_case_authority`
    FOREIGN KEY (`authority_id`) REFERENCES `authorities` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_case_parent`
    FOREIGN KEY (`parent_case_id`) REFERENCES `cases` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Tabla central de casos legales y contables';

DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `trg_cases_before_insert`
BEFORE INSERT ON `cases`
FOR EACH ROW
BEGIN
  IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
    SET NEW.uuid = UUID();
  END IF;
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Tabla: case_collaborators — Usuarios colaboradores en un caso
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `case_collaborators` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_id`     BIGINT UNSIGNED NOT NULL,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `can_edit`    TINYINT(1)      NOT NULL DEFAULT 1  COMMENT '1 = puede actualizar el caso',
  `added_by`    BIGINT UNSIGNED NULL,
  `added_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `removed_at`  DATETIME        NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_case_collab` (`case_id`, `user_id`),
  KEY `idx_collab_case` (`case_id`),
  KEY `idx_collab_user` (`user_id`),
  CONSTRAINT `fk_collab_case`
    FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_collab_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB COMMENT='Colaboradores asignados a cada caso';

-- -----------------------------------------------------------------------------
-- Tabla: case_alert_emails — Emails adicionales para alertas del caso
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `case_alert_emails` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `case_id`    BIGINT UNSIGNED NOT NULL,
  `email`      VARCHAR(180)    NOT NULL,
  `label`      VARCHAR(100)    NULL COMMENT 'Ej: "Gerente", "Cliente 2"',
  `added_by`   BIGINT UNSIGNED NULL,
  `added_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_case_alert_email` (`case_id`, `email`),
  CONSTRAINT `fk_alert_case`
    FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='Correos adicionales para alertas de un caso específico';

-- =============================================================================
-- SECCIÓN 5: VERSIONES Y COMENTARIOS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: case_versions — Actualizaciones / versiones de un caso
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `case_versions` (
  `id`              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
  `uuid`            CHAR(36)         NOT NULL,
  `case_id`         BIGINT UNSIGNED  NOT NULL,
  `version_number`  SMALLINT UNSIGNED NOT NULL COMMENT 'Correlativo 1, 2, 3...',
  `title`           VARCHAR(300)     NOT NULL,
  `description`     TEXT             NOT NULL,
  `status_id`       TINYINT UNSIGNED NOT NULL  COMMENT 'Estado resultante de esta versión',
  `created_by`      BIGINT UNSIGNED  NOT NULL,
  `is_deleted`      TINYINT(1)       NOT NULL DEFAULT 0  COMMENT 'Solo SYS_ADMIN puede borrar',
  `deleted_by`      BIGINT UNSIGNED  NULL,
  `deleted_at`      DATETIME         NULL,
  `pdf_s3_key`      VARCHAR(500)     NULL  COMMENT 'Ruta del PDF generado en S3',
  `created_at`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_version_uuid`   (`uuid`),
  UNIQUE KEY `uq_version_number` (`case_id`, `version_number`),
  KEY `idx_version_case`   (`case_id`),
  KEY `idx_version_author` (`created_by`),
  KEY `idx_version_status` (`status_id`),
  CONSTRAINT `fk_version_case`
    FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`),
  CONSTRAINT `fk_version_author`
    FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_version_status`
    FOREIGN KEY (`status_id`) REFERENCES `case_statuses` (`id`)
) ENGINE=InnoDB COMMENT='Versiones / actualizaciones de cada caso (inmutables para usuarios)';

DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `trg_versions_before_insert`
BEFORE INSERT ON `case_versions`
FOR EACH ROW
BEGIN
  IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
    SET NEW.uuid = UUID();
  END IF;
  -- Asignar número de versión automáticamente
  SET NEW.version_number = (
    SELECT COALESCE(MAX(version_number), 0) + 1
    FROM case_versions
    WHERE case_id = NEW.case_id
  );
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Tabla: case_comments — Comentarios en versiones
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `case_comments` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid`        CHAR(36)        NOT NULL,
  `version_id`  BIGINT UNSIGNED NOT NULL,
  `case_id`     BIGINT UNSIGNED NOT NULL  COMMENT 'Desnormalizado para queries rápidos',
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `content`     TEXT            NOT NULL,
  `is_deleted`  TINYINT(1)      NOT NULL DEFAULT 0  COMMENT 'Solo SYS_ADMIN puede borrar',
  `deleted_by`  BIGINT UNSIGNED NULL,
  `deleted_at`  DATETIME        NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_comment_uuid` (`uuid`),
  KEY `idx_comment_version` (`version_id`),
  KEY `idx_comment_case`    (`case_id`),
  KEY `idx_comment_user`    (`user_id`),
  CONSTRAINT `fk_comment_version`
    FOREIGN KEY (`version_id`) REFERENCES `case_versions` (`id`),
  CONSTRAINT `fk_comment_case`
    FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`),
  CONSTRAINT `fk_comment_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB COMMENT='Comentarios en cada versión del caso';

DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `trg_comments_before_insert`
BEFORE INSERT ON `case_comments`
FOR EACH ROW
BEGIN
  IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
    SET NEW.uuid = UUID();
  END IF;
END$$
DELIMITER ;

-- =============================================================================
-- SECCIÓN 6: DOCUMENTOS Y REPOSITORIOS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: documents — Archivos adjuntos a casos y versiones
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `documents` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid`           CHAR(36)        NOT NULL,
  `case_id`        BIGINT UNSIGNED NOT NULL,
  `version_id`     BIGINT UNSIGNED NULL  COMMENT 'NULL = adjunto al caso directamente',
  `name`           VARCHAR(300)    NOT NULL  COMMENT 'Nombre original del archivo',
  `mime_type`      VARCHAR(100)    NOT NULL,
  `size_bytes`     BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `storage`        ENUM('S3','GOOGLE_DRIVE') NOT NULL DEFAULT 'S3',
  `s3_bucket`      VARCHAR(200)    NULL,
  `s3_key`         VARCHAR(500)    NULL,
  `drive_file_id`  VARCHAR(255)    NULL,
  `drive_url`      VARCHAR(500)    NULL,
  `folder_path`    VARCHAR(500)    NULL  COMMENT 'Ruta de la carpeta en el repositorio',
  `is_evidence`    TINYINT(1)      NOT NULL DEFAULT 0  COMMENT '1 = evidencia de cierre',
  `uploaded_by`    BIGINT UNSIGNED NULL,
  `is_deleted`     TINYINT(1)      NOT NULL DEFAULT 0,
  `deleted_by`     BIGINT UNSIGNED NULL,
  `deleted_at`     DATETIME        NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_document_uuid` (`uuid`),
  KEY `idx_doc_case`    (`case_id`),
  KEY `idx_doc_version` (`version_id`),
  KEY `idx_doc_storage` (`storage`),
  CONSTRAINT `fk_doc_case`
    FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`),
  CONSTRAINT `fk_doc_version`
    FOREIGN KEY (`version_id`) REFERENCES `case_versions` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_doc_uploader`
    FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Documentos adjuntos a casos y versiones';

DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `trg_documents_before_insert`
BEFORE INSERT ON `documents`
FOR EACH ROW
BEGIN
  IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
    SET NEW.uuid = UUID();
  END IF;
END$$
DELIMITER ;

-- -----------------------------------------------------------------------------
-- Tabla: repositories — Carpetas en Google Drive / S3
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `repositories` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`           VARCHAR(300)    NOT NULL,
  `type`           ENUM('CASE','RELATED_CASES','GENERAL','ADMIN_PRIVATE') NOT NULL,
  `storage`        ENUM('GOOGLE_DRIVE','S3') NOT NULL,
  `drive_folder_id` VARCHAR(255)   NULL,
  `drive_url`      VARCHAR(500)    NULL,
  `s3_bucket`      VARCHAR(200)    NULL,
  `s3_prefix`      VARCHAR(500)    NULL,
  `case_id`        BIGINT UNSIGNED NULL  COMMENT 'Para tipo CASE',
  `related_case_id` BIGINT UNSIGNED NULL COMMENT 'Para tipo RELATED_CASES (segundo caso)',
  `created_by`     BIGINT UNSIGNED NULL,
  `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_repo_case` (`case_id`),
  KEY `idx_repo_type` (`type`),
  CONSTRAINT `fk_repo_case`
    FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_repo_related`
    FOREIGN KEY (`related_case_id`) REFERENCES `cases` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Carpetas en Google Drive y S3 para cada caso';

-- =============================================================================
-- SECCIÓN 7: RELACIONES ENTRE CASOS
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: case_relations — Relaciones padre-hijo entre casos
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `case_relations` (
  `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `parent_case_id`     BIGINT UNSIGNED NOT NULL,
  `child_case_id`      BIGINT UNSIGNED NOT NULL,
  `relation_type`      ENUM('PARENT_CHILD','RELATED') NOT NULL DEFAULT 'PARENT_CHILD',
  `shared_folder_name` VARCHAR(300)    NULL  COMMENT 'EXP001--EXP002',
  `repository_id`      BIGINT UNSIGNED NULL  COMMENT 'Carpeta compartida creada',
  `assigned_by`        BIGINT UNSIGNED NOT NULL  COMMENT 'Solo SYS_ADMIN',
  `assigned_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `notes`              TEXT            NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_case_relation` (`parent_case_id`, `child_case_id`),
  KEY `idx_rel_parent` (`parent_case_id`),
  KEY `idx_rel_child`  (`child_case_id`),
  CONSTRAINT `fk_rel_parent`
    FOREIGN KEY (`parent_case_id`) REFERENCES `cases` (`id`),
  CONSTRAINT `fk_rel_child`
    FOREIGN KEY (`child_case_id`) REFERENCES `cases` (`id`),
  CONSTRAINT `fk_rel_repo`
    FOREIGN KEY (`repository_id`) REFERENCES `repositories` (`id`) ON DELETE SET NULL,
  CONSTRAINT `chk_no_self_relation`
    CHECK (`parent_case_id` != `child_case_id`)
) ENGINE=InnoDB COMMENT='Relaciones entre casos (padre-hijo / correlacionados)';

-- =============================================================================
-- SECCIÓN 8: COLABORACIÓN PÚBLICA DE CLIENTES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: client_collaborations — Solicitudes públicas sin registro
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `client_collaborations` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid`            CHAR(36)        NOT NULL  COMMENT 'Token público para la URL',
  `case_id`         BIGINT UNSIGNED NOT NULL,
  `request_reason`  TEXT            NOT NULL  COMMENT 'Por qué se solicita la colaboración',
  `client_name`     VARCHAR(200)    NULL  COMMENT 'Llenado por el cliente',
  `client_phone`    VARCHAR(25)     NULL,
  `client_address`  TEXT            NULL,
  `client_message`  TEXT            NULL,
  `doc_s3_key`      VARCHAR(500)    NULL  COMMENT 'Documento adjunto por el cliente',
  `responded_at`    DATETIME        NULL,
  `is_active`       TINYINT(1)      NOT NULL DEFAULT 1,
  `created_by`      BIGINT UNSIGNED NULL,
  `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_collab_uuid` (`uuid`),
  KEY `idx_collab_case` (`case_id`),
  CONSTRAINT `fk_client_collab_case`
    FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`)
) ENGINE=InnoDB COMMENT='Vista pública de colaboración cliente — sin autenticación';

DELIMITER $$
CREATE TRIGGER IF NOT EXISTS `trg_client_collab_before_insert`
BEFORE INSERT ON `client_collaborations`
FOR EACH ROW
BEGIN
  IF NEW.uuid IS NULL OR NEW.uuid = '' THEN
    SET NEW.uuid = UUID();
  END IF;
END$$
DELIMITER ;

-- =============================================================================
-- SECCIÓN 9: NOTIFICACIONES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: notifications — Centro de notificaciones del sistema
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notifications` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NOT NULL,
  `case_id`     BIGINT UNSIGNED NULL,
  `type`        ENUM(
                  'CASE_CREATED','CASE_UPDATED','CASE_ASSIGNED',
                  'VERSION_PUBLISHED','COMMENT_ADDED',
                  'INACTIVITY_ALERT','DEADLINE_REMINDER',
                  'COLLABORATION_REQUEST','ROLE_ASSIGNED',
                  'SYSTEM_ALERT'
                ) NOT NULL,
  `channel`     ENUM('IN_APP','EMAIL','SMS','PUSH') NOT NULL DEFAULT 'IN_APP',
  `title`       VARCHAR(200)    NOT NULL,
  `message`     TEXT            NOT NULL,
  `is_read`     TINYINT(1)      NOT NULL DEFAULT 0,
  `read_at`     DATETIME        NULL,
  `sent_at`     DATETIME        NULL,
  `error_msg`   VARCHAR(500)    NULL,
  `metadata`    JSON            NULL  COMMENT 'Datos extra según el tipo',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notif_user`     (`user_id`, `is_read`),
  KEY `idx_notif_case`     (`case_id`),
  KEY `idx_notif_type`     (`type`),
  KEY `idx_notif_created`  (`created_at`),
  CONSTRAINT `fk_notif_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notif_case`
    FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Centro de notificaciones in-app y registro de envíos email/SMS';

-- =============================================================================
-- SECCIÓN 10: AUDITORÍA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: audit_logs — Registro inmutable de acciones críticas
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`     BIGINT UNSIGNED NULL,
  `action`      VARCHAR(100)    NOT NULL  COMMENT 'Ej: CASE_DELETED, ROLE_CHANGED',
  `entity_type` VARCHAR(50)     NOT NULL  COMMENT 'Ej: cases, users, comments',
  `entity_id`   BIGINT UNSIGNED NULL,
  `old_values`  JSON            NULL,
  `new_values`  JSON            NULL,
  `ip_address`  VARCHAR(45)     NULL,
  `user_agent`  VARCHAR(512)    NULL,
  `notes`       TEXT            NULL,
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user`   (`user_id`),
  KEY `idx_audit_entity` (`entity_type`, `entity_id`),
  KEY `idx_audit_action` (`action`),
  KEY `idx_audit_date`   (`created_at`),
  CONSTRAINT `fk_audit_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='Registro de auditoría — nunca se borra';

-- =============================================================================
-- SECCIÓN 11: ERRORES CUSTOMIZADOS DEL SISTEMA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabla: system_error_configs — Errores customizados (RN-S-014)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_error_configs` (
  `id`           INT UNSIGNED    NOT NULL AUTO_INCREMENT,
  `error_code`   VARCHAR(50)     NOT NULL  COMMENT 'Ej: ERR_CASE_NOT_FOUND',
  `http_status`  SMALLINT        NOT NULL  DEFAULT 500,
  `title_es`     VARCHAR(200)    NOT NULL,
  `message_es`   TEXT            NOT NULL,
  `title_en`     VARCHAR(200)    NULL,
  `message_en`   TEXT            NULL,
  `notify_admin` TINYINT(1)      NOT NULL DEFAULT 0,
  `is_active`    TINYINT(1)      NOT NULL DEFAULT 1,
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_error_code` (`error_code`)
) ENGINE=InnoDB COMMENT='Configuración de errores customizados del sistema';

INSERT INTO `system_error_configs` (`error_code`, `http_status`, `title_es`, `message_es`) VALUES
  ('ERR_AUTH_INVALID_CREDENTIALS',  401, 'Credenciales inválidas',       'El usuario o contraseña son incorrectos.'),
  ('ERR_AUTH_DEVICE_NOT_TRUSTED',   403, 'Dispositivo no autorizado',    'Este dispositivo no está registrado para tu cuenta.'),
  ('ERR_AUTH_MAX_DEVICES',          403, 'Límite de dispositivos',       'Has alcanzado el máximo de 2 dispositivos para tu cuenta.'),
  ('ERR_CASE_NOT_FOUND',            404, 'Caso no encontrado',           'El caso solicitado no existe o no tienes acceso.'),
  ('ERR_CASE_CATEGORY_MISMATCH',    403, 'Categoría incorrecta',         'No tienes acceso a casos de esta categoría.'),
  ('ERR_CASE_ALREADY_CERTIFIED',    409, 'Caso certificado',             'Un caso certificado no puede ser reabierto.'),
  ('ERR_CASE_INACTIVITY',           422, 'Caso inactivo',                'Este caso lleva más de 3 días sin actualización.'),
  ('ERR_VERSION_IMMUTABLE',         403, 'Versión inmutable',            'Solo el administrador del sistema puede eliminar versiones.'),
  ('ERR_COMMENT_IMMUTABLE',         403, 'Comentario inmutable',         'Solo el administrador del sistema puede eliminar comentarios.'),
  ('ERR_UPLOAD_MAX_FILES',          400, 'Límite de archivos',           'Solo se permiten hasta 10 archivos por operación de carga.'),
  ('ERR_USER_PENDING_ROLE',         403, 'Rol pendiente',                'Tu cuenta está pendiente de asignación de rol. Contacta al administrador.'),
  ('ERR_BUCKET_ACCESS_DENIED',      403, 'Acceso denegado al bucket',    'No tienes permisos para acceder a este bucket.'),
  ('ERR_RELATION_SAME_CASE',        400, 'Relación inválida',            'Un caso no puede relacionarse consigo mismo.')
ON DUPLICATE KEY UPDATE `title_es` = VALUES(`title_es`);

-- =============================================================================
-- SECCIÓN 12: VISTAS ÚTILES
-- =============================================================================

CREATE OR REPLACE VIEW `v_cases_summary` AS
SELECT
  c.id,
  c.uuid,
  c.case_number,
  c.title,
  cat.name          AS category,
  cs.name           AS status,
  cl.full_name      AS client_name,
  cl.phone          AS client_phone,
  CONCAT(u.first_name, ' ', u.last_name) AS owner_name,
  a.name            AS authority_name,
  c.estimated_end_date,
  c.created_at,
  c.updated_at,
  (SELECT MAX(cv.created_at)
   FROM case_versions cv WHERE cv.case_id = c.id AND cv.is_deleted = 0)
                    AS last_version_at,
  (SELECT COUNT(*) FROM case_versions cv WHERE cv.case_id = c.id AND cv.is_deleted = 0)
                    AS version_count,
  (SELECT COUNT(*) FROM case_collaborators cc WHERE cc.case_id = c.id AND cc.removed_at IS NULL)
                    AS collaborator_count,
  DATEDIFF(NOW(), (SELECT MAX(cv2.created_at) FROM case_versions cv2 WHERE cv2.case_id = c.id))
                    AS days_since_last_version
FROM cases c
JOIN categories   cat ON c.category_id = cat.id
JOIN case_statuses cs  ON c.status_id  = cs.id
JOIN clients      cl  ON c.client_id   = cl.id
JOIN users        u   ON c.owner_id    = u.id
LEFT JOIN authorities a ON c.authority_id = a.id
WHERE c.deleted_at IS NULL;

CREATE OR REPLACE VIEW `v_cases_overdue` AS
SELECT * FROM `v_cases_summary`
WHERE days_since_last_version >= 3
  AND status NOT IN ('CERTIFIED', 'FINISHED_SUCCESS', 'FINISHED_FAIL');

CREATE OR REPLACE VIEW `v_user_active_cases` AS
SELECT
  u.id       AS user_id,
  u.uuid     AS user_uuid,
  CONCAT(u.first_name, ' ', u.last_name) AS user_name,
  c.id       AS case_id,
  c.uuid     AS case_uuid,
  c.case_number,
  c.title,
  'OWNER'    AS relationship,
  cs.name    AS status
FROM users u
JOIN cases c  ON c.owner_id = u.id AND c.deleted_at IS NULL
JOIN case_statuses cs ON c.status_id = cs.id
UNION ALL
SELECT
  u.id,
  u.uuid,
  CONCAT(u.first_name, ' ', u.last_name),
  c.id,
  c.uuid,
  c.case_number,
  c.title,
  'COLLABORATOR',
  cs.name
FROM users u
JOIN case_collaborators cc ON cc.user_id = u.id AND cc.removed_at IS NULL
JOIN cases c  ON c.id = cc.case_id AND c.deleted_at IS NULL
JOIN case_statuses cs ON c.status_id = cs.id;

-- =============================================================================
-- SECCIÓN 13: STORED PROCEDURES
-- =============================================================================

DELIMITER $$

-- Publicar una nueva versión de un caso
CREATE PROCEDURE IF NOT EXISTS `sp_publish_version`(
  IN  p_case_id    BIGINT UNSIGNED,
  IN  p_user_id    BIGINT UNSIGNED,
  IN  p_title      VARCHAR(300),
  IN  p_description TEXT,
  IN  p_status_id  TINYINT UNSIGNED,
  OUT p_version_id BIGINT UNSIGNED,
  OUT p_result     VARCHAR(200)
)
BEGIN
  DECLARE v_user_can_edit TINYINT DEFAULT 0;
  DECLARE v_case_exists   TINYINT DEFAULT 0;
  DECLARE v_status_code   VARCHAR(30);

  -- Verificar si el caso existe y no está eliminado
  SELECT COUNT(*) INTO v_case_exists
  FROM cases WHERE id = p_case_id AND deleted_at IS NULL;

  IF v_case_exists = 0 THEN
    SET p_result = 'ERROR:ERR_CASE_NOT_FOUND';
    LEAVE sp_label;
  END IF;

  -- Verificar status actual (no se puede actualizar un caso certificado)
  SELECT cs.code INTO v_status_code
  FROM cases c JOIN case_statuses cs ON c.status_id = cs.id
  WHERE c.id = p_case_id;

  IF v_status_code = 'CERTIFIED' THEN
    SET p_result = 'ERROR:ERR_CASE_ALREADY_CERTIFIED';
    LEAVE sp_label;
  END IF;

  -- Verificar que el usuario tiene permiso (es dueño o colaborador con can_edit)
  SELECT COUNT(*) INTO v_user_can_edit FROM (
    SELECT id FROM cases WHERE id = p_case_id AND owner_id = p_user_id
    UNION
    SELECT case_id FROM case_collaborators
    WHERE case_id = p_case_id AND user_id = p_user_id AND can_edit = 1 AND removed_at IS NULL
  ) AS allowed;

  -- También admins
  SELECT v_user_can_edit + COUNT(*) INTO v_user_can_edit
  FROM users u JOIN roles r ON u.role_id = r.id
  WHERE u.id = p_user_id AND r.code IN ('SYS_ADMIN', 'ADMIN', 'SUPERVISOR');

  IF v_user_can_edit = 0 THEN
    SET p_result = 'ERROR:ERR_CASE_CATEGORY_MISMATCH';
    LEAVE sp_label;
  END IF;

  -- Insertar versión
  INSERT INTO case_versions (case_id, title, description, status_id, created_by)
  VALUES (p_case_id, p_title, p_description, p_status_id, p_user_id);

  SET p_version_id = LAST_INSERT_ID();

  -- Actualizar estado del caso
  UPDATE cases
  SET status_id = p_status_id, updated_at = NOW()
  WHERE id = p_case_id;

  SET p_result = 'OK';
END$$

-- Obtener el timeline completo de un caso
CREATE PROCEDURE IF NOT EXISTS `sp_get_case_timeline`(
  IN p_case_id BIGINT UNSIGNED
)
BEGIN
  SELECT
    cv.id           AS version_id,
    cv.version_number,
    cv.title,
    cv.description,
    cs.name         AS status,
    cs.code         AS status_code,
    CONCAT(u.first_name, ' ', u.last_name) AS author,
    cv.created_at,
    cv.is_deleted,
    (SELECT JSON_ARRAYAGG(
       JSON_OBJECT(
         'id', cc.id,
         'author', CONCAT(cu.first_name, ' ', cu.last_name),
         'content', cc.content,
         'created_at', cc.created_at,
         'is_deleted', cc.is_deleted
       )
     )
     FROM case_comments cc
     JOIN users cu ON cc.user_id = cu.id
     WHERE cc.version_id = cv.id
    ) AS comments,
    (SELECT JSON_ARRAYAGG(
       JSON_OBJECT(
         'id', d.id,
         'name', d.name,
         'mime_type', d.mime_type,
         'size_bytes', d.size_bytes,
         'storage', d.storage
       )
     )
     FROM documents d
     WHERE d.version_id = cv.id AND d.is_deleted = 0
    ) AS documents
  FROM case_versions cv
  JOIN users u        ON cv.created_by = u.id
  JOIN case_statuses cs ON cv.status_id = cs.id
  WHERE cv.case_id = p_case_id
  ORDER BY cv.version_number DESC;
END$$

DELIMITER ;

-- =============================================================================
-- SECCIÓN 14: USUARIO ADMINISTRADOR INICIAL
-- =============================================================================

-- Insertar usuario SYS_ADMIN inicial
INSERT INTO `users` (
  `uuid`, `email`, `email_verified`, `password_hash`,
  `first_name`, `last_name`, `phone`,
  `role_id`, `category_id`, `status`, `oauth_provider`
)
SELECT
  UUID(),
  'sysadmin@legal-system.com',
  1,
  -- BCrypt hash de 'Ant0n14BM87' — cámbiala después del primer login
  '$2a$12$hJiGJXYwk3R.Pv7e5mPKze6JKsC9JhQv8Jbi4r5gW4VtF1y8qF5uy',
  'System',
  'Admin',
  '+10000000000',
  (SELECT id FROM roles WHERE code = 'SYS_ADMIN'),
  NULL,
  'ACTIVE',
  'LOCAL'
WHERE NOT EXISTS (
  SELECT 1 FROM users WHERE email = 'sysadmin@legal-system.com'
);

-- =============================================================================
-- RESTAURAR CONFIGURACIÓN
-- =============================================================================
SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- =============================================================================
-- RESUMEN DE TABLAS CREADAS
-- =============================================================================
SELECT
  TABLE_NAME        AS `Tabla`,
  TABLE_ROWS        AS `Filas aprox.`,
  TABLE_COMMENT     AS `Descripción`
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'legal_system_db'
ORDER BY TABLE_NAME;
