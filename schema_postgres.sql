-- =========================================================================
-- PostgreSQL Database Schema
-- Based on the UML Class Diagram for EE_CV_PROJECT
-- =========================================================================

-- Enable UUID extension in case UUIDs are needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Drop existing tables to ensure clean setup (order matters due to FK constraints)
DROP TABLE IF EXISTS cv_skills CASCADE;
DROP TABLE IF EXISTS job_required_skills CASCADE;
DROP TABLE IF EXISTS job_offer_skills CASCADE;
DROP TABLE IF EXISTS skills CASCADE;
DROP TABLE IF EXISTS applications CASCADE;
DROP TABLE IF EXISTS cvs CASCADE;
DROP TABLE IF EXISTS job_offers CASCADE;
DROP TABLE IF EXISTS recruiters CASCADE;
DROP TABLE IF EXISTS candidates CASCADE;
DROP TABLE IF EXISTS admins CASCADE;
DROP TABLE IF EXISTS app_users CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS application_status CASCADE;

-- =========================================================================
-- ENUMERATIONS
-- =========================================================================

-- Role enumeration mapping
CREATE TYPE user_role AS ENUM ('ADMIN', 'RECRUITER', 'CANDIDATE');

-- ApplicationStatus enumeration mapping
CREATE TYPE application_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');

-- =========================================================================
-- ENTITIES & TABLES (JOINED INHERITANCE PATTERN FOR USER HIERARCHY)
-- =========================================================================

-- Base class: User (app_users table)
CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

-- Subclass Candidate: inherits from User (joined on candidate_id)
CREATE TABLE candidates (
    id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    phone VARCHAR(30),
    address VARCHAR(255)
);

-- Subclass Recruiter: inherits from User (joined on recruiter_id)
CREATE TABLE recruiters (
    id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE,
    company_name VARCHAR(150)
);

-- Subclass Admin: inherits from User (joined on admin_id)
CREATE TABLE admins (
    id BIGINT PRIMARY KEY REFERENCES app_users(id) ON DELETE CASCADE
);

-- Entity: CV (owns relationship with Candidate)
CREATE TABLE cvs (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    candidate_id BIGINT UNIQUE NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    ai_improvements TEXT
);

-- Entity: JobOffer (published by Recruiter)
CREATE TABLE job_offers (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    location VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recruiter_id BIGINT NOT NULL REFERENCES recruiters(id) ON DELETE CASCADE
);

-- Entity: Application (submitted by Candidate for a JobOffer)
CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    application_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    score DOUBLE PRECISION,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    candidate_id BIGINT NOT NULL REFERENCES candidates(id) ON DELETE CASCADE,
    job_offer_id BIGINT NOT NULL REFERENCES job_offers(id) ON DELETE CASCADE,
    ai_strongest_points TEXT,
    CONSTRAINT unique_candidate_offer UNIQUE (candidate_id, job_offer_id)
);

-- Entity: Skill
CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

-- =========================================================================
-- RELATIONSHIPS / ASSOCIATION TABLES (MANY-TO-MANY)
-- =========================================================================

-- Relationship: CV contains Skills
CREATE TABLE cv_skills (
    cv_id BIGINT REFERENCES cvs(id) ON DELETE CASCADE,
    skill_id BIGINT REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (cv_id, skill_id)
);

-- Relationship: JobOffer requires Skills
CREATE TABLE job_offer_skills (
    job_offer_id BIGINT REFERENCES job_offers(id) ON DELETE CASCADE,
    skill_id BIGINT REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (job_offer_id, skill_id)
);

-- Compatibility / Legacy Support Table for JPA @ElementCollection requiredSkills
CREATE TABLE job_required_skills (
    job_offer_id BIGINT REFERENCES job_offers(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL,
    PRIMARY KEY (job_offer_id, skill)
);

-- =========================================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- =========================================================================
CREATE INDEX idx_users_email ON app_users(email);
CREATE INDEX idx_cvs_candidate ON cvs(candidate_id);
CREATE INDEX idx_applications_candidate ON applications(candidate_id);
CREATE INDEX idx_applications_offer ON applications(job_offer_id);
CREATE INDEX idx_job_offers_recruiter ON job_offers(recruiter_id);
