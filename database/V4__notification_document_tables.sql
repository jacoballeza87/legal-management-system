-- ============================================================
-- V4__notification_document_tables.sql
-- Tablas para notification-service y document-service
-- ============================================================

-- ─── NOTIFICATION SERVICE ────────────────────────────────────

CREATE TABLE IF NOT EXISTS notifications (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id   BIGINT        NOT NULL,
    recipient_email     VARCHAR(100)  NOT NULL,
    recipient_phone     VARCHAR(20),
    subject             VARCHAR(200)  NOT NULL,
    body                TEXT          NOT NULL,
    type                ENUM('EMAIL','SMS','IN_APP','PUSH') NOT NULL,
    status              ENUM('PENDING','SENT','FAILED','READ') NOT NULL DEFAULT 'PENDING',
    priority            ENUM('LOW','MEDIUM','HIGH','CRITICAL') DEFAULT 'MEDIUM',
    entity_type         VARCHAR(50),
    entity_id           BIGINT,
    event_type          VARCHAR(100),
    metadata            TEXT,
    retry_count         INT           DEFAULT 0,
    error_message       VARCHAR(500),
    sent_at             DATETIME,
    read_at             DATETIME,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_user   (recipient_user_id),
    INDEX idx_notif_status (status),
    INDEX idx_notif_entity (entity_type, entity_id)
);

CREATE TABLE IF NOT EXISTS email_notifications (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id  BIGINT       NOT NULL,
    to_email         VARCHAR(200) NOT NULL,
    cc_emails        TEXT,
    bcc_emails       TEXT,
    from_email       VARCHAR(200) NOT NULL,
    from_name        VARCHAR(100),
    subject          VARCHAR(200) NOT NULL,
    html_body        LONGTEXT,
    text_body        LONGTEXT,
    template_name    VARCHAR(100),
    message_id       VARCHAR(200),
    ses_request_id   VARCHAR(200),
    email_status     ENUM('QUEUED','SENT','BOUNCED','COMPLAINED','DELIVERY_FAILED') DEFAULT 'QUEUED',
    sent_at          DATETIME,
    error_details    TEXT,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    INDEX idx_email_notif (notification_id)
);

CREATE TABLE IF NOT EXISTS sms_notifications (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id   BIGINT      NOT NULL,
    to_phone_number   VARCHAR(20) NOT NULL,
    from_phone_number VARCHAR(20) NOT NULL,
    message_body      TEXT        NOT NULL,
    twilio_sid        VARCHAR(100),
    twilio_status     VARCHAR(50),
    sms_status        ENUM('QUEUED','SENT','DELIVERED','FAILED','UNDELIVERED') DEFAULT 'QUEUED',
    sent_at           DATETIME,
    error_details     TEXT,
    FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE
);

-- ─── DOCUMENT SERVICE ────────────────────────────────────────

CREATE TABLE IF NOT EXISTS s3_buckets (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    bucket_name      VARCHAR(100) NOT NULL UNIQUE,
    region           VARCHAR(50)  NOT NULL,
    purpose          VARCHAR(50),
    versioning       BOOLEAN DEFAULT FALSE,
    public_access    BOOLEAN DEFAULT FALSE,
    lifecycle_policy TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS s3_folders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    bucket_id        BIGINT NOT NULL,
    folder_path      VARCHAR(500) NOT NULL,
    display_name     VARCHAR(200),
    case_id          BIGINT,
    case_number      VARCHAR(50),
    file_count       BIGINT DEFAULT 0,
    total_size_bytes BIGINT DEFAULT 0,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bucket_id) REFERENCES s3_buckets(id)
);

CREATE TABLE IF NOT EXISTS case_documents (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id                 BIGINT       NOT NULL,
    case_number             VARCHAR(50)  NOT NULL,
    document_key            VARCHAR(50)  NOT NULL UNIQUE,
    original_file_name      VARCHAR(300) NOT NULL,
    mime_type               VARCHAR(100) NOT NULL,
    file_size               BIGINT       NOT NULL,
    s3_key                  VARCHAR(500) NOT NULL,
    s3_bucket_name          VARCHAR(100) NOT NULL,
    google_drive_file_id    VARCHAR(200),
    google_drive_folder_id  VARCHAR(200),
    description             VARCHAR(500),
    uploaded_by_user_id     BIGINT       NOT NULL,
    uploaded_by_name        VARCHAR(200),
    status                  ENUM('UPLOADING','ACTIVE','ARCHIVED','DELETED','VIRUS_DETECTED')
                            NOT NULL DEFAULT 'ACTIVE',
    category                ENUM('CONTRATO','DEMANDA','SENTENCIA','EVIDENCIA','CORRESPONDENCIA',
                                 'FACTURA','PODER_NOTARIAL','IDENTIFICACION','OTRO') NOT NULL,
    version                 INT DEFAULT 1,
    previous_version_id     BIGINT,
    checksum                VARCHAR(64),
    uploaded_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at              DATETIME,
    INDEX idx_doc_case   (case_id),
    INDEX idx_doc_key    (document_key),
    INDEX idx_doc_status (status)
);

CREATE TABLE IF NOT EXISTS s3_files (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    folder_id   BIGINT NOT NULL,
    document_id BIGINT,
    s3_key      VARCHAR(500) NOT NULL,
    file_name   VARCHAR(300) NOT NULL,
    mime_type   VARCHAR(100),
    size_bytes  BIGINT,
    etag        VARCHAR(100),
    version_id  INT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (folder_id)   REFERENCES s3_folders(id),
    FOREIGN KEY (document_id) REFERENCES case_documents(id)
);

-- Bucket por defecto
INSERT IGNORE INTO s3_buckets (bucket_name, region, purpose, versioning, public_access)
VALUES ('legal-management-docs', 'us-east-1', 'documents', TRUE, FALSE);
