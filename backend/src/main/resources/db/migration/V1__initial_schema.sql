-- CivicConnect Database Schema — V1 Initial Schema
-- Created: 2026-10-09
-- Matches JPA entities in com.civicconnect.model

-- ============================================================================
-- EXTENSIONS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- USERS TABLE
-- ============================================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(30) NOT NULL DEFAULT 'CITIZEN' CHECK (role IN ('CITIZEN','OFFICER','DEPARTMENT_HEAD','ADMIN')),
    department_id   UUID,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_department_id ON users(department_id);

-- ============================================================================
-- DEPARTMENTS TABLE
-- ============================================================================
CREATE TABLE departments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL UNIQUE,
    code            VARCHAR(50) NOT NULL UNIQUE,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_departments_code ON departments(code);
CREATE INDEX idx_departments_name ON departments(name);

-- Add FK from users to departments (after both tables exist)
ALTER TABLE users
    ADD CONSTRAINT fk_users_department
    FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;

-- ============================================================================
-- COMPLAINTS TABLE
-- ============================================================================
CREATE TABLE complaints (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number        VARCHAR(20) NOT NULL UNIQUE,
    tracking_code           VARCHAR(64) NOT NULL UNIQUE,
    title                   VARCHAR(255) NOT NULL,
    category                VARCHAR(50) NOT NULL CHECK (category IN ('GARBAGE_COLLECTION','ROADS_AND_POTHOLES','DRAINAGE_PROBLEMS','STREETLIGHT_ISSUES','WATER_SUPPLY')),
    description             TEXT NOT NULL,
    locality                VARCHAR(255) NOT NULL,
    landmark                VARCHAR(255),
    status                  VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED','UNDER_REVIEW','IN_PROGRESS','RESOLVED','REJECTED','REOPENED')),
    submitted_by            UUID,
    submitter_contact       VARCHAR(255),
    assigned_department_id  UUID,
    remarks                 TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_complaints_status ON complaints(status);
CREATE INDEX idx_complaints_submitted_by ON complaints(submitted_by);
CREATE INDEX idx_complaints_reference ON complaints(reference_number);
CREATE INDEX idx_complaints_tracking_code ON complaints(tracking_code);
CREATE INDEX idx_complaints_assigned_department ON complaints(assigned_department_id);
CREATE INDEX idx_complaints_category ON complaints(category);
CREATE INDEX idx_complaints_created_at ON complaints(created_at DESC);

ALTER TABLE complaints
    ADD CONSTRAINT fk_complaints_submitted_by
    FOREIGN KEY (submitted_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE complaints
    ADD CONSTRAINT fk_complaints_assigned_department
    FOREIGN KEY (assigned_department_id) REFERENCES departments(id) ON DELETE SET NULL;

-- ============================================================================
-- COMPLAINT ASSIGNMENTS TABLE
-- ============================================================================
CREATE TABLE complaint_assignments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id    UUID NOT NULL,
    officer_id      UUID NOT NULL,
    assigned_by     UUID NOT NULL,
    assigned_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_ca_complaint_id ON complaint_assignments(complaint_id);
CREATE INDEX idx_ca_officer_id ON complaint_assignments(officer_id);
CREATE INDEX idx_ca_assigned_by ON complaint_assignments(assigned_by);
CREATE INDEX idx_ca_active ON complaint_assignments(complaint_id, is_active) WHERE is_active = TRUE;

ALTER TABLE complaint_assignments
    ADD CONSTRAINT fk_ca_complaint
    FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE;

ALTER TABLE complaint_assignments
    ADD CONSTRAINT fk_ca_officer
    FOREIGN KEY (officer_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE complaint_assignments
    ADD CONSTRAINT fk_ca_assigned_by
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE CASCADE;

-- ============================================================================
-- COMPLAINT ATTACHMENTS TABLE
-- ============================================================================
CREATE TABLE complaint_attachments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id    UUID NOT NULL,
    storage_path    VARCHAR(500) NOT NULL,
    file_name       VARCHAR(255) NOT NULL,
    mime_type       VARCHAR(100),
    size_bytes      BIGINT,
    uploaded_by     UUID,
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_catt_complaint_id ON complaint_attachments(complaint_id);
CREATE INDEX idx_catt_uploaded_by ON complaint_attachments(uploaded_by);

ALTER TABLE complaint_attachments
    ADD CONSTRAINT fk_catt_complaint
    FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE;

ALTER TABLE complaint_attachments
    ADD CONSTRAINT fk_catt_uploaded_by
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL;

-- ============================================================================
-- COMPLAINT STATUS HISTORY TABLE
-- ============================================================================
CREATE TABLE complaint_status_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID NOT NULL,
    from_status VARCHAR(30) CHECK (from_status IN ('SUBMITTED','UNDER_REVIEW','IN_PROGRESS','RESOLVED','REJECTED','REOPENED')),
    to_status   VARCHAR(30) NOT NULL CHECK (to_status IN ('SUBMITTED','UNDER_REVIEW','IN_PROGRESS','RESOLVED','REJECTED','REOPENED')),
    changed_by  UUID,
    note        TEXT,
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_csh_complaint_id ON complaint_status_history(complaint_id);
CREATE INDEX idx_csh_changed_at ON complaint_status_history(changed_at);

ALTER TABLE complaint_status_history
    ADD CONSTRAINT fk_csh_complaint
    FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE;

ALTER TABLE complaint_status_history
    ADD CONSTRAINT fk_csh_changed_by
    FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL;

-- ============================================================================
-- NOTIFICATIONS TABLE
-- ============================================================================
CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID,
    complaint_id        UUID,
    notification_type   VARCHAR(50) NOT NULL,
    message             TEXT NOT NULL,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX idx_notifications_complaint_id ON notifications(complaint_id);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_complaint
    FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE;

-- ============================================================================
-- AUDIT LOG TABLE
-- ============================================================================
CREATE TABLE audit_log (
    id              BIGSERIAL PRIMARY KEY,
    actor_id        UUID,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       UUID,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at DESC);

ALTER TABLE audit_log
    ADD CONSTRAINT fk_audit_log_actor
    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL;

-- ============================================================================
-- UPDATED_AT TRIGGER FUNCTION
-- ============================================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tables with updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_complaints_updated_at
    BEFORE UPDATE ON complaints
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();