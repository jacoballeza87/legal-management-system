-- =========================================
-- SISTEMA DE GESTIÓN LEGAL Y CONTABLE
-- Schema Principal - MySQL 8.0
-- =========================================

-- Crear base de datos
CREATE DATABASE IF NOT EXISTS legal_management_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE legal_management_db;

-- =========================================
-- TABLA: users - Usuarios del sistema
-- =========================================
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    alternative_email VARCHAR(255),
    role_id BIGINT,
    category_id BIGINT,
    status ENUM('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED') DEFAULT 'PENDING',
    auth_provider ENUM('LOCAL', 'GOOGLE', 'MICROSOFT') DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    temporary_password BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    created_by BIGINT,
    INDEX idx_email (email),
    INDEX idx_role (role_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: roles - Roles del sistema
-- =========================================
CREATE TABLE roles (
    role_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    is_system_admin BOOLEAN DEFAULT FALSE,
    is_supervisor BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: categories - Categorías (Contable/Jurídico)
-- =========================================
CREATE TABLE categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    category_code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: permissions - Permisos del sistema
-- =========================================
CREATE TABLE permissions (
    permission_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(100) NOT NULL UNIQUE,
    permission_code VARCHAR(50) NOT NULL UNIQUE,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resource_action (resource, action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: role_permissions - Relación roles-permisos
-- =========================================
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(permission_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: device_registrations - Dispositivos registrados
-- =========================================
CREATE TABLE device_registrations (
    device_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_identifier VARCHAR(255) NOT NULL,
    device_name VARCHAR(255),
    device_type VARCHAR(50),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_device (user_id, device_identifier),
    INDEX idx_user_active (user_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: cases - Casos legales/contables
-- =========================================
CREATE TABLE cases (
    case_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_number VARCHAR(100) NOT NULL UNIQUE,
    expedient_number VARCHAR(100) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    category_id BIGINT NOT NULL,
    
    -- Información del cliente
    client_name VARCHAR(255) NOT NULL,
    client_address TEXT NOT NULL,
    client_phone VARCHAR(20) NOT NULL,
    client_email VARCHAR(255) NOT NULL,
    
    -- Información legal
    against_party VARCHAR(255),
    court_number VARCHAR(100),
    authority_name VARCHAR(255),
    authority_type ENUM('LABORAL', 'CIVIL', 'MERCANTIL', 'SUPREMA_CORTE', 'OTRO'),
    authority_address TEXT,
    
    -- Asignación
    owner_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    
    -- Estado y fechas
    status ENUM(
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
    ) DEFAULT 'CREATED',
    
    estimated_completion_date DATE,
    actual_completion_date DATE,
    
    -- Relaciones entre casos
    parent_case_id BIGINT NULL,
    
    -- Configuración
    enable_client_collaboration BOOLEAN DEFAULT FALSE,
    client_collaboration_description TEXT,
    
    -- QR Code
    qr_code_url VARCHAR(500),
    qr_code_token VARCHAR(255) UNIQUE,
    
    -- Repositorio
    drive_folder_id VARCHAR(255),
    correlation_folder_id VARCHAR(255),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_version_date TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    
    FOREIGN KEY (category_id) REFERENCES categories(category_id),
    FOREIGN KEY (owner_id) REFERENCES users(user_id),
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    FOREIGN KEY (parent_case_id) REFERENCES cases(case_id) ON DELETE SET NULL,
    
    INDEX idx_case_number (case_number),
    INDEX idx_expedient (expedient_number),
    INDEX idx_owner (owner_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    INDEX idx_parent (parent_case_id),
    INDEX idx_qr_token (qr_code_token),
    INDEX idx_last_version (last_version_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: case_collaborators - Colaboradores de casos
-- =========================================
CREATE TABLE case_collaborators (
    collaboration_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    added_by BIGINT NOT NULL,
    role ENUM('COLLABORATOR', 'VIEWER') DEFAULT 'COLLABORATOR',
    can_edit BOOLEAN DEFAULT TRUE,
    can_add_versions BOOLEAN DEFAULT TRUE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES cases(case_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (added_by) REFERENCES users(user_id),
    UNIQUE KEY unique_case_user (case_id, user_id),
    INDEX idx_case (case_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: case_versions - Versiones/Actualizaciones de casos
-- =========================================
CREATE TABLE case_versions (
    version_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    created_by BIGINT NOT NULL,
    previous_status ENUM(
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
    ),
    new_status ENUM(
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
    ),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES cases(case_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    UNIQUE KEY unique_case_version (case_id, version_number),
    INDEX idx_case (case_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: version_comments - Comentarios en versiones
-- =========================================
CREATE TABLE version_comments (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_by BIGINT NULL,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (version_id) REFERENCES case_versions(version_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (deleted_by) REFERENCES users(user_id),
    INDEX idx_version (version_id),
    INDEX idx_user (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: case_documents - Documentos adjuntos a casos
-- =========================================
CREATE TABLE case_documents (
    document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    version_id BIGINT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    storage_path VARCHAR(1000) NOT NULL,
    storage_type ENUM('GOOGLE_DRIVE', 'S3') DEFAULT 'GOOGLE_DRIVE',
    drive_file_id VARCHAR(255),
    s3_key VARCHAR(500),
    uploaded_by BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_evidence BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (case_id) REFERENCES cases(case_id) ON DELETE CASCADE,
    FOREIGN KEY (version_id) REFERENCES case_versions(version_id) ON DELETE SET NULL,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id),
    INDEX idx_case (case_id),
    INDEX idx_version (version_id),
    INDEX idx_upload_date (upload_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: case_relations - Relaciones entre casos (padre-hijo)
-- =========================================
CREATE TABLE case_relations (
    relation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_case_id BIGINT NOT NULL,
    child_case_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_case_id) REFERENCES cases(case_id) ON DELETE CASCADE,
    FOREIGN KEY (child_case_id) REFERENCES cases(case_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    UNIQUE KEY unique_relation (parent_case_id, child_case_id),
    INDEX idx_parent (parent_case_id),
    INDEX idx_child (child_case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: client_collaborations - Colaboraciones de clientes
-- =========================================
CREATE TABLE client_collaborations (
    collaboration_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    client_phone VARCHAR(20) NOT NULL,
    client_address TEXT NOT NULL,
    description TEXT NOT NULL,
    document_path VARCHAR(1000),
    status ENUM('PENDING', 'COMPLETED', 'REJECTED') DEFAULT 'PENDING',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES cases(case_id) ON DELETE CASCADE,
    INDEX idx_case (case_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: notifications - Notificaciones del sistema
-- =========================================
CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    case_id BIGINT NULL,
    type ENUM(
        'CASE_CREATED',
        'CASE_ASSIGNED',
        'NEW_VERSION',
        'NEW_COMMENT',
        'INACTIVITY_ALERT',
        'STATUS_CHANGE',
        'COLLABORATION_REQUEST',
        'SYSTEM'
    ) NOT NULL,
    title VARCHAR(500) NOT NULL,
    message TEXT NOT NULL,
    channel ENUM('EMAIL', 'SMS', 'IN_APP', 'ALL') DEFAULT 'IN_APP',
    is_read BOOLEAN DEFAULT FALSE,
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    email_sent BOOLEAN DEFAULT FALSE,
    sms_sent BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (case_id) REFERENCES cases(case_id) ON DELETE SET NULL,
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_case (case_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: email_notifications - Log de emails enviados
-- =========================================
CREATE TABLE email_notifications (
    email_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    has_attachment BOOLEAN DEFAULT FALSE,
    attachment_path VARCHAR(1000),
    status ENUM('PENDING', 'SENT', 'FAILED') DEFAULT 'PENDING',
    sent_at TIMESTAMP NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(notification_id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: sms_notifications - Log de SMS enviados
-- =========================================
CREATE TABLE sms_notifications (
    sms_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NULL,
    phone_number VARCHAR(20) NOT NULL,
    message TEXT NOT NULL,
    status ENUM('PENDING', 'SENT', 'FAILED') DEFAULT 'PENDING',
    sent_at TIMESTAMP NULL,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (notification_id) REFERENCES notifications(notification_id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: s3_buckets - Buckets de S3 (administración)
-- =========================================
CREATE TABLE s3_buckets (
    bucket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bucket_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: s3_folders - Carpetas en buckets
-- =========================================
CREATE TABLE s3_folders (
    folder_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bucket_id BIGINT NOT NULL,
    parent_folder_id BIGINT NULL,
    folder_name VARCHAR(255) NOT NULL,
    folder_path VARCHAR(1000) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bucket_id) REFERENCES s3_buckets(bucket_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES s3_folders(folder_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id),
    INDEX idx_bucket (bucket_id),
    INDEX idx_parent (parent_folder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: s3_files - Archivos en S3
-- =========================================
CREATE TABLE s3_files (
    file_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    folder_id BIGINT NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    s3_key VARCHAR(500) NOT NULL,
    uploaded_by BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    can_modify BOOLEAN DEFAULT TRUE,
    can_delete BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (folder_id) REFERENCES s3_folders(folder_id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_id),
    INDEX idx_folder (folder_id),
    INDEX idx_upload_date (upload_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: audit_log - Log de auditoría
-- =========================================
CREATE TABLE audit_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    old_values JSON,
    new_values JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABLA: system_settings - Configuración del sistema
-- =========================================
CREATE TABLE system_settings (
    setting_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    data_type ENUM('STRING', 'INTEGER', 'BOOLEAN', 'JSON') DEFAULT 'STRING',
    description TEXT,
    updated_by BIGINT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by) REFERENCES users(user_id),
    INDEX idx_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- DATOS INICIALES
-- =========================================

-- Insertar categorías
INSERT INTO categories (category_name, category_code, description) VALUES
('Contable', 'ACCOUNTING', 'Casos relacionados con contabilidad, finanzas y auditoría'),
('Jurídico', 'LEGAL', 'Casos relacionados con asuntos legales y jurídicos');

-- Insertar roles
INSERT INTO roles (role_name, role_code, description, is_system_admin, is_supervisor) VALUES
('Administrador del Sistema', 'SYSTEM_ADMIN', 'Control total del sistema', TRUE, FALSE),
('Supervisor Contable', 'SUPERVISOR_ACCOUNTING', 'Supervisor de área contable', FALSE, TRUE),
('Supervisor Jurídico', 'SUPERVISOR_LEGAL', 'Supervisor de área jurídica', FALSE, TRUE),
('Administrador', 'ADMIN', 'Administrador general', FALSE, FALSE),
('Abogado', 'LAWYER', 'Usuario jurídico', FALSE, FALSE),
('Contador', 'ACCOUNTANT', 'Usuario contable', FALSE, FALSE),
('Colaborador', 'COLLABORATOR', 'Colaborador en casos', FALSE, FALSE),
('Pendiente', 'PENDING', 'Usuario sin rol asignado', FALSE, FALSE);

-- Insertar permisos base
INSERT INTO permissions (permission_name, permission_code, resource, action, description) VALUES
-- Casos
('Ver Casos', 'VIEW_CASES', 'cases', 'view', 'Ver casos según categoría'),
('Crear Casos', 'CREATE_CASES', 'cases', 'create', 'Crear nuevos casos'),
('Editar Casos', 'EDIT_CASES', 'cases', 'edit', 'Editar casos existentes'),
('Eliminar Casos', 'DELETE_CASES', 'cases', 'delete', 'Eliminar casos'),
('Asignar Casos', 'ASSIGN_CASES', 'cases', 'assign', 'Asignar casos a usuarios'),

-- Usuarios
('Ver Usuarios', 'VIEW_USERS', 'users', 'view', 'Ver usuarios del sistema'),
('Crear Usuarios', 'CREATE_USERS', 'users', 'create', 'Crear nuevos usuarios'),
('Editar Usuarios', 'EDIT_USERS', 'users', 'edit', 'Editar usuarios existentes'),
('Eliminar Usuarios', 'DELETE_USERS', 'users', 'delete', 'Eliminar usuarios'),
('Asignar Roles', 'ASSIGN_ROLES', 'users', 'assign_role', 'Asignar roles a usuarios'),

-- Versiones
('Crear Versiones', 'CREATE_VERSIONS', 'versions', 'create', 'Crear versiones de casos'),
('Editar Versiones', 'EDIT_VERSIONS', 'versions', 'edit', 'Editar versiones'),
('Eliminar Versiones', 'DELETE_VERSIONS', 'versions', 'delete', 'Eliminar versiones'),

-- Comentarios
('Crear Comentarios', 'CREATE_COMMENTS', 'comments', 'create', 'Crear comentarios'),
('Eliminar Comentarios', 'DELETE_COMMENTS', 'comments', 'delete', 'Eliminar comentarios'),

-- S3/Buckets
('Administrar S3', 'MANAGE_S3', 's3', 'manage', 'Administración completa de S3'),
('Ver S3', 'VIEW_S3', 's3', 'view', 'Ver archivos en S3'),
('Subir S3', 'UPLOAD_S3', 's3', 'upload', 'Subir archivos a S3');

-- Asignar permisos a roles (ejemplos - se deben completar según necesidad)
-- SYSTEM_ADMIN: Todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, permission_id FROM permissions;

-- Configuración del sistema
INSERT INTO system_settings (setting_key, setting_value, data_type, description) VALUES
('MAX_DEVICES_SYSTEM_ADMIN', '2', 'INTEGER', 'Máximo de dispositivos para administrador del sistema'),
('MAX_DEVICES_ADMIN', '5', 'INTEGER', 'Máximo de dispositivos para administradores'),
('INACTIVITY_ALERT_DAYS', '3', 'INTEGER', 'Días de inactividad antes de alerta'),
('MAX_FILES_PER_UPLOAD', '10', 'INTEGER', 'Máximo de archivos por carga'),
('ENABLE_SMS_NOTIFICATIONS', 'true', 'BOOLEAN', 'Habilitar notificaciones por SMS'),
('ENABLE_EMAIL_NOTIFICATIONS', 'true', 'BOOLEAN', 'Habilitar notificaciones por email');

-- =========================================
-- ÍNDICES ADICIONALES PARA OPTIMIZACIÓN
-- =========================================
CREATE INDEX idx_cases_owner_status ON cases(owner_id, status);
CREATE INDEX idx_cases_category_status ON cases(category_id, status);
CREATE INDEX idx_versions_case_date ON case_versions(case_id, created_at);
CREATE INDEX idx_notifications_user_type ON notifications(user_id, type, is_read);
CREATE INDEX idx_audit_user_date ON audit_log(user_id, created_at);

-- =========================================
-- TRIGGERS
-- =========================================

-- Trigger para actualizar last_version_date en cases
DELIMITER //
CREATE TRIGGER update_case_last_version
AFTER INSERT ON case_versions
FOR EACH ROW
BEGIN
    UPDATE cases 
    SET last_version_date = NEW.created_at,
        status = NEW.new_status
    WHERE case_id = NEW.case_id;
END//
DELIMITER ;

-- Trigger para crear auditoría en cambios de caso
DELIMITER //
CREATE TRIGGER audit_case_update
AFTER UPDATE ON cases
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (user_id, action, entity_type, entity_id, old_values, new_values)
    VALUES (
        NEW.owner_id,
        'UPDATE',
        'case',
        NEW.case_id,
        JSON_OBJECT('status', OLD.status, 'owner_id', OLD.owner_id),
        JSON_OBJECT('status', NEW.status, 'owner_id', NEW.owner_id)
    );
END//
DELIMITER ;

-- =========================================
-- VISTAS ÚTILES
-- =========================================

-- Vista de casos con información completa
CREATE VIEW v_cases_full AS
SELECT 
    c.*,
    cat.category_name,
    owner.full_name as owner_name,
    owner.email as owner_email,
    creator.full_name as creator_name,
    parent.case_number as parent_case_number,
    (SELECT COUNT(*) FROM case_versions WHERE case_id = c.case_id) as version_count,
    (SELECT COUNT(*) FROM case_collaborators WHERE case_id = c.case_id) as collaborator_count,
    (SELECT COUNT(*) FROM case_documents WHERE case_id = c.case_id) as document_count
FROM cases c
JOIN categories cat ON c.category_id = cat.category_id
JOIN users owner ON c.owner_id = owner.user_id
JOIN users creator ON c.created_by = creator.user_id
LEFT JOIN cases parent ON c.parent_case_id = parent.case_id;

-- Vista de usuarios con roles
CREATE VIEW v_users_with_roles AS
SELECT 
    u.*,
    r.role_name,
    r.role_code,
    cat.category_name,
    cat.category_code
FROM users u
LEFT JOIN roles r ON u.role_id = r.role_id
LEFT JOIN categories cat ON u.category_id = cat.category_id;

-- =========================================
-- FIN DEL SCHEMA
-- =========================================
