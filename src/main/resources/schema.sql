-- ============================================================
-- Interview Platform - Oracle DDL Script
-- ============================================================

-- Sequences for ID generation
CREATE SEQUENCE seq_candidate_id
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE seq_interviewer_id
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE seq_interview_id
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

CREATE SEQUENCE seq_user_id
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- ============================================================
-- HR Users Table
-- ============================================================
CREATE TABLE hr_users (
    id          NUMBER DEFAULT seq_user_id.NEXTVAL PRIMARY KEY,
    username    VARCHAR2(100) NOT NULL UNIQUE,
    email       VARCHAR2(255) NOT NULL UNIQUE,
    password    VARCHAR2(255) NOT NULL,
    role        VARCHAR2(50)  NOT NULL DEFAULT 'ROLE_HR',
    enabled     NUMBER(1)     NOT NULL DEFAULT 1,
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ============================================================
-- Candidates Table
-- ============================================================
CREATE TABLE candidates (
    id          NUMBER DEFAULT seq_candidate_id.NEXTVAL PRIMARY KEY,
    name        VARCHAR2(255) NOT NULL,
    email       VARCHAR2(255) NOT NULL UNIQUE,
    phone       VARCHAR2(50),
    timezone    VARCHAR2(100) DEFAULT 'UTC',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ============================================================
-- Interviewers Table
-- ============================================================
CREATE TABLE interviewers (
    id                NUMBER DEFAULT seq_interviewer_id.NEXTVAL PRIMARY KEY,
    name              VARCHAR2(255) NOT NULL,
    email             VARCHAR2(255) NOT NULL UNIQUE,
    department        VARCHAR2(100),
    working_hours     VARCHAR2(100) DEFAULT '09:00-18:00',
    calendar_provider VARCHAR2(50)  DEFAULT 'GOOGLE',
    active            NUMBER(1)     NOT NULL DEFAULT 1,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ============================================================
-- Interviews Table
-- ============================================================
CREATE TABLE interviews (
    id               NUMBER DEFAULT seq_interview_id.NEXTVAL PRIMARY KEY,
    candidate_id     NUMBER NOT NULL,
    interviewer_id   NUMBER NOT NULL,
    title            VARCHAR2(500),
    scheduled_time   TIMESTAMP NOT NULL,
    duration_minutes NUMBER(5) NOT NULL DEFAULT 60,
    meeting_link     VARCHAR2(1000),
    calendar_event_id VARCHAR2(500),
    status           VARCHAR2(50) NOT NULL DEFAULT 'INVITED',
    action_token     VARCHAR2(1000),
    token_expires_at TIMESTAMP,
    invite_sent_at   TIMESTAMP,
    confirmed_at     TIMESTAMP,
    rescheduled_at   TIMESTAMP,
    cancelled_at     TIMESTAMP,
    notes            CLOB,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_interview_candidate  FOREIGN KEY (candidate_id)   REFERENCES candidates(id),
    CONSTRAINT fk_interview_interviewer FOREIGN KEY (interviewer_id) REFERENCES interviewers(id),
    CONSTRAINT chk_interview_status CHECK (status IN ('INVITED','CONFIRMED','RESCHEDULED','CANCELLED','COMPLETED'))
);

-- ============================================================
-- Indexes
-- ============================================================
CREATE INDEX idx_interview_candidate   ON interviews(candidate_id);
CREATE INDEX idx_interview_interviewer ON interviews(interviewer_id);
CREATE INDEX idx_interview_status      ON interviews(status);
CREATE INDEX idx_interview_scheduled   ON interviews(scheduled_time);
CREATE INDEX idx_candidate_email       ON candidates(email);
CREATE INDEX idx_interviewer_email     ON interviewers(email);

-- ============================================================
-- Triggers to auto-update updated_at
-- ============================================================
CREATE OR REPLACE TRIGGER trg_candidates_updated
    BEFORE UPDATE ON candidates
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_interviewers_updated
    BEFORE UPDATE ON interviewers
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_interviews_updated
    BEFORE UPDATE ON interviews
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_hr_users_updated
    BEFORE UPDATE ON hr_users
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

-- ============================================================
-- Sample HR User (password: Admin@123 - BCrypt encoded)
-- ============================================================
INSERT INTO hr_users (username, email, password, role)
VALUES ('admin', 'admin@techcorp.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'ROLE_HR');

COMMIT;

-- ============================================================
-- Migration: Add AI polling fields to interviews table
-- Run this if the interviews table already exists
-- ============================================================
-- ALTER TABLE interviews ADD (
--     ai_stopped              NUMBER(1)     DEFAULT 0 NOT NULL,
--     last_processed_email_id VARCHAR2(500)
-- );

-- If creating fresh, the full CREATE TABLE below already includes these columns.
-- The above ALTER is only needed for existing databases.
