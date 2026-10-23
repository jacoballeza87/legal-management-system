-- Create users and roles
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    role VARCHAR(50) NOT NULL -- ADMIN, JURIDICO, CONTABLE, SYSADMIN, CLIENTE
);

-- Core Case Table (RN-F-001)
CREATE TABLE legal_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dossier_number VARCHAR(255) UNIQUE NOT NULL,
    assignee_id BIGINT,
    category VARCHAR(50) NOT NULL,
    client_info TEXT,
    opponent VARCHAR(255),
    authority VARCHAR(255),
    authority_address VARCHAR(255),
    status VARCHAR(50) NOT NULL, -- RN-F-003
    estimated_date DATE, -- RN-F-005
    parent_case_id BIGINT NULL, -- RN-F-009 (Relación Padre-Hijo)
    client_collaboration_enabled BOOLEAN DEFAULT FALSE, -- RN-O-009
    collaboration_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (assignee_id) REFERENCES users(id),
    FOREIGN KEY (parent_case_id) REFERENCES legal_cases(id)
);

-- Case Collaborators (RN-F-007)
CREATE TABLE case_collaborators (
    case_id BIGINT,
    user_id BIGINT,
    PRIMARY KEY (case_id, user_id),
    FOREIGN KEY (case_id) REFERENCES legal_cases(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Versions/Updates (RN-O-001, RN-O-003)
CREATE TABLE case_updates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comments TEXT,
    version_number INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (case_id) REFERENCES legal_cases(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
