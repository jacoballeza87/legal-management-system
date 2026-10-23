CREATE DATABASE IF NOT EXISTS legal_management;
USE legal_management;

-- USER SERVICE DB
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(50) NOT NULL -- ADMIN, JURIDICO, CONTABLE, SYSADMIN, CLIENTE
);

-- CASE SERVICE DB
CREATE TABLE legal_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dossier_number VARCHAR(255) UNIQUE NOT NULL, -- RN-F-001
    assignee_id BIGINT,
    category VARCHAR(50) NOT NULL, 
    client_info TEXT,
    opponent VARCHAR(255),
    authority VARCHAR(255),
    authority_address VARCHAR(255),
    status VARCHAR(50) NOT NULL, -- RN-F-003
    estimated_date DATE,
    parent_case_id BIGINT NULL, -- RN-F-009: Casos Padre-Hijo
    shared_folder_id VARCHAR(255), -- RN-O-007: Carpeta compartida
    client_collaboration_enabled BOOLEAN DEFAULT FALSE, -- RN-O-009
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE case_collaborators (
    case_id BIGINT,
    user_id BIGINT,
    PRIMARY KEY (case_id, user_id)
);

CREATE TABLE case_updates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comments TEXT,
    version_number INT NOT NULL, -- RN-O-001
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
