-- =============================================================================
-- SAFE MIGRATION: legal_management_db
-- Run this ONCE against your existing RDS MySQL instance
-- All statements use IF NOT EXISTS / IF EXISTS — safe to run on partial schema
-- Engine: MySQL 8.0
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- =============================================================================
-- 1. USERS TABLE — Missing columns
-- =============================================================================

-- Fix temporary_password: was BOOLEAN, needs to store actual hash
ALTER TABLE users MODIFY COLUMN temporary_password VARCHAR(255) NULL
  COMMENT 'Hashed temp password for OAuth users (BR#29)';

-- Add missing columns one by one (safe if already exist via IF NOT EXISTS workaround)
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS temp_password_expires_at  DATETIME         NULL         COMMENT 'Expiry for temp password (BR#29)',
  ADD COLUMN IF NOT EXISTS email_verified             TINYINT(1)       NOT NULL DEFAULT 0 COMMENT 'Email verified flag (BR#29)',
  ADD COLUMN IF NOT EXISTS failed_login_attempts      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Brute force counter (BR#44)',
  ADD COLUMN IF NOT EXISTS locked_until               DATETIME         NULL         COMMENT 'Account locked until (BR#44)',
  ADD COLUMN IF NOT EXISTS uuid                       CHAR(36)         NULL         COMMENT 'Public-facing UUID for APIs — never expose user_id',
  ADD COLUMN IF NOT EXISTS deleted_at                 DATETIME         NULL         COMMENT 'Soft delete (BR#12)';

-- Backfill uuid for existing users
UPDATE users SET uuid = UUID() WHERE uuid IS NULL;

-- Make uuid unique after backfill
ALTER TABLE users
  ADD UNIQUE KEY IF NOT EXISTS uq_user_uuid (uuid);

-- =============================================================================
-- 2. CASES TABLE — Missing columns + status ENUM fix
-- =============================================================================

-- Add missing columns
ALTER TABLE cases
  ADD COLUMN IF NOT EXISTS supervisor_id    BIGINT       NULL COMMENT 'Supervisor asignado (BR#26)',
  ADD COLUMN IF NOT EXISTS certified_at     DATETIME     NULL COMMENT 'Fecha de certificación (BR#36)',
  ADD COLUMN IF NOT EXISTS certified_by     BIGINT       NULL COMMENT 'Usuario que certificó (BR#36)',
  ADD COLUMN IF NOT EXISTS deleted_at       DATETIME     NULL COMMENT 'Soft delete — solo admin (BR#12)',
  ADD COLUMN IF NOT EXISTS deleted_by       BIGINT       NULL COMMENT 'Admin que eliminó el caso';

-- Fix status ENUM to include CERTIFIED and NOT_CREATED
-- MySQL requires full ENUM redefinition
ALTER TABLE cases
  MODIFY COLUMN status ENUM(
    'NOT_CREATED',
    'CREATED',
    'ASSIGNED',
    'UPDATED',
    'IN_APPROVAL',
    'CLOSED',
    'COMPLETED',
    'FINISHED_UNSUCCESSFULLY',
    'FINISHED_SUCCESSFULLY',
    'REOPENED',
    'CERTIFIED'
  ) NOT NULL DEFAULT 'CREATED';

-- Index for supervisor
ALTER TABLE cases
  ADD INDEX IF NOT EXISTS idx_supervisor (supervisor_id),
  ADD INDEX IF NOT EXISTS idx_deleted    (deleted_at);

-- =============================================================================
-- 3. CASE_VERSIONS TABLE — Missing columns
-- =============================================================================

ALTER TABLE case_versions
  ADD COLUMN IF NOT EXISTS extra_notification_email VARCHAR(255) NULL    COMMENT 'Optional extra email per version (BR#15)',
  ADD COLUMN IF NOT EXISTS pdf_url                  VARCHAR(500) NULL    COMMENT 'Generated PDF URL (BR#11,#53)',
  ADD COLUMN IF NOT EXISTS pdf_generated            TINYINT(1)   NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS is_deleted               TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Admin-only delete (BR#12)',
  ADD COLUMN IF NOT EXISTS deleted_by               BIGINT       NULL,
  ADD COLUMN IF NOT EXISTS deleted_at               DATETIME     NULL;

-- =============================================================================
-- 4. S3_BUCKETS TABLE — Add bucket type for two-repo distinction (BR#20, #40)
-- =============================================================================

ALTER TABLE s3_buckets
  ADD COLUMN IF NOT EXISTS bucket_type   ENUM('CASES','CLOUD','GENERAL') NOT NULL DEFAULT 'GENERAL'
    COMMENT 'CASES=case docs, CLOUD=sysadmin private (BR#40), GENERAL=shared repo (BR#20)',
  ADD COLUMN IF NOT EXISTS is_private    TINYINT(1) NOT NULL DEFAULT 0
    COMMENT '1 = SYS_ADMIN only (BR#40)',
  ADD COLUMN IF NOT EXISTS region        VARCHAR(50) NULL DEFAULT 'us-east-1',
  ADD COLUMN IF NOT EXISTS updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- =============================================================================
-- 5. S3_FILES TABLE — Link files to cases and versions (BR#30, #31)
-- =============================================================================

ALTER TABLE s3_files
  ADD COLUMN IF NOT EXISTS case_id      BIGINT       NULL COMMENT 'Direct case link (BR#30)',
  ADD COLUMN IF NOT EXISTS version_id   BIGINT       NULL COMMENT 'Linked to specific version (BR#30)',
  ADD COLUMN IF NOT EXISTS is_evidence  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Evidence on closed case (BR#31)',
  ADD COLUMN IF NOT EXISTS deleted_at   DATETIME     NULL;

-- =============================================================================
-- 6. NEW TABLE: authorities — Catalog of juzgados/tribunales (BR#10)
-- =============================================================================

CREATE TABLE IF NOT EXISTS authorities (
  authority_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name           VARCHAR(255)    NOT NULL,
  type           ENUM('LABORAL','CIVIL','MERCANTIL','SUPREMA_CORTE','FEDERAL','ADMINISTRATIVE','OTHER')
                                 NOT NULL DEFAULT 'OTHER',
  court_number   VARCHAR(100)    NULL,
  state          VARCHAR(100)    NULL,
  city           VARCHAR(100)    NULL,
  address        TEXT            NULL  COMMENT 'Exact address fetched via BR#10',
  phone          VARCHAR(25)     NULL,
  email          VARCHAR(180)    NULL,
  maps_url       VARCHAR(500)    NULL  COMMENT 'Google Maps link',
  is_active      TINYINT(1)      NOT NULL DEFAULT 1,
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (authority_id),
  FULLTEXT KEY ft_authority_name (name)
) ENGINE=InnoDB COMMENT='Catálogo de autoridades y juzgados (BR#10)';

-- Link cases to authorities catalog
ALTER TABLE cases
  ADD COLUMN IF NOT EXISTS authority_id BIGINT UNSIGNED NULL
    COMMENT 'FK to authorities catalog (BR#10)';

-- =============================================================================
-- 7. NEW TABLE: case_inactivity_alerts — Track 3-day alert sends (BR#27)
-- =============================================================================

CREATE TABLE IF NOT EXISTS case_inactivity_alerts (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  case_id      BIGINT          NOT NULL,
  alert_type   ENUM('EMAIL','SMS','BOTH') NOT NULL DEFAULT 'BOTH',
  sent_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_to      VARCHAR(500)    NULL COMMENT 'JSON array of recipients',
  PRIMARY KEY (id),
  KEY idx_case_alert (case_id),
  KEY idx_sent_at    (sent_at)
) ENGINE=InnoDB COMMENT='Log of 3-day inactivity alerts (BR#27)';

-- =============================================================================
-- 8. NEW TABLE: bucket_permissions — Role-based S3 access (BR#54)
-- =============================================================================

CREATE TABLE IF NOT EXISTS bucket_permissions (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  bucket_id    BIGINT          NOT NULL,
  role_id      BIGINT          NOT NULL,
  can_view     TINYINT(1)      NOT NULL DEFAULT 1,
  can_upload   TINYINT(1)      NOT NULL DEFAULT 0,
  can_modify   TINYINT(1)      NOT NULL DEFAULT 0,
  can_delete   TINYINT(1)      NOT NULL DEFAULT 0,
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_bucket_role (bucket_id, role_id)
) ENGINE=InnoDB COMMENT='Role-based S3 bucket permissions (BR#54)';

-- =============================================================================
-- 9. NEW TABLE: error_catalog — Custom error registry (BR#49)
-- =============================================================================

CREATE TABLE IF NOT EXISTS error_catalog (
  id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  error_code       VARCHAR(50)     NOT NULL UNIQUE,
  http_status      SMALLINT        NOT NULL DEFAULT 500,
  title            VARCHAR(200)    NOT NULL,
  message_template TEXT            NOT NULL COMMENT 'Supports {variables}',
  notify_admin     TINYINT(1)      NOT NULL DEFAULT 0,
  is_active        TINYINT(1)      NOT NULL DEFAULT 1,
  created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Custom error catalog (BR#49)';

-- Seed common errors
INSERT IGNORE INTO error_catalog (error_code, http_status, title, message_template, notify_admin) VALUES
  ('ERR_USER_NOT_FOUND',       404, 'Usuario no encontrado',          'El usuario con ID {id} no existe.',                        0),
  ('ERR_CASE_NOT_FOUND',       404, 'Caso no encontrado',             'El caso {case_number} no existe o fue eliminado.',         0),
  ('ERR_CASE_CERTIFIED',       409, 'Caso certificado',               'El caso {case_number} está certificado y no puede modificarse.', 0),
  ('ERR_NO_PERMISSION',        403, 'Sin permisos',                   'No tienes permisos para realizar esta acción.',            0),
  ('ERR_CATEGORY_MISMATCH',    403, 'Categoría incorrecta',           'Tu categoría no permite acceder a casos de {category}.',   0),
  ('ERR_DEVICE_LIMIT',         409, 'Límite de dispositivos',         'Has alcanzado el límite de {max} dispositivos registrados.', 0),
  ('ERR_INACTIVITY_ALERT',     200, 'Alerta de inactividad',          'El caso {case_number} lleva {days} días sin actualización.', 1),
  ('ERR_TEMP_PASSWORD_EXPIRED',401, 'Contraseña temporal expirada',   'Tu contraseña temporal ha expirado. Solicita una nueva.',  0),
  ('ERR_EMAIL_NOT_VERIFIED',   401, 'Email no verificado',            'Debes verificar tu correo electrónico antes de continuar.',0);

-- =============================================================================
-- 10. FIX: client_collaborations — Add review tracking (BR#38)
-- =============================================================================

ALTER TABLE client_collaborations
  ADD COLUMN IF NOT EXISTS reviewed_by   BIGINT   NULL COMMENT 'Admin who reviewed',
  ADD COLUMN IF NOT EXISTS reviewed_at   DATETIME NULL;

-- =============================================================================
-- 11. SYSTEM SETTINGS — Add missing settings
-- =============================================================================

INSERT IGNORE INTO system_settings (setting_key, setting_value, data_type, description) VALUES
  ('INACTIVITY_ALERT_DAYS',        '3',     'INTEGER', 'Days without version before alert (BR#27)'),
  ('MAX_DEVICES_SYS_ADMIN',        '2',     'INTEGER', 'Max devices for SYS_ADMIN (BR#44)'),
  ('MAX_DEVICES_ADMIN',            '5',     'INTEGER', 'Max devices for ADMIN (BR#44)'),
  ('MAX_FILES_PER_UPLOAD',         '10',    'INTEGER', 'Max files per upload batch (BR#48)'),
  ('TEMP_PASSWORD_EXPIRY_HOURS',   '24',    'INTEGER', 'Hours before temp password expires (BR#29)'),
  ('QR_BASE_URL',                  '',      'STRING',  'Base URL for QR code deep links (BR#16)'),
  ('PDF_STORAGE_BUCKET',           '',      'STRING',  'S3 bucket name for generated PDFs (BR#11)'),
  ('ENABLE_WHATSAPP_SHARE',        'true',  'BOOLEAN', 'Enable QR share via WhatsApp (BR#16)'),
  ('ENABLE_SMS_NOTIFICATIONS',     'true',  'BOOLEAN', 'Enable SMS notifications (BR#27)'),
  ('ENABLE_EMAIL_NOTIFICATIONS',   'true',  'BOOLEAN', 'Enable email notifications');

-- =============================================================================
-- 12. RE-ENABLE FOREIGN KEY CHECKS
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- VERIFICATION QUERIES — Run these after migration to confirm success
-- =============================================================================

SELECT 'USERS columns' AS check_target,
  COUNT(*) AS found_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'legal_management_db'
  AND TABLE_NAME = 'users'
  AND COLUMN_NAME IN ('uuid','email_verified','temp_password_expires_at',
                      'failed_login_attempts','locked_until','deleted_at');

SELECT 'CASES columns' AS check_target,
  COUNT(*) AS found_columns
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'legal_management_db'
  AND TABLE_NAME = 'cases'
  AND COLUMN_NAME IN ('supervisor_id','certified_at','certified_by','deleted_at','authority_id');

SELECT 'New tables' AS check_target, TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'legal_management_db'
  AND TABLE_NAME IN ('authorities','case_inactivity_alerts','bucket_permissions','error_catalog')
ORDER BY TABLE_NAME;

SELECT 'CASES status enum has CERTIFIED' AS check_target,
  COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'legal_management_db'
  AND TABLE_NAME = 'cases'
  AND COLUMN_NAME = 'status';
