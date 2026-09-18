-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 07_auth_extension.sql
-- Description: Non-destructive extension of USERS table for Firebase Google
--              Authentication and application-level roles (USER, ADMIN).
-- ============================================================================

-- 1. Modify USERS.password to be NULLable for Firebase Google users
ALTER TABLE USERS MODIFY password NULL;

-- 2. Add firebase_uid for stable Firebase identity mapping
ALTER TABLE USERS ADD firebase_uid VARCHAR2(128);
ALTER TABLE USERS ADD CONSTRAINT uq_users_firebase_uid UNIQUE (firebase_uid);

-- 3. Add role with default 'USER' and check constraint ('USER', 'ADMIN')
ALTER TABLE USERS ADD role VARCHAR2(20) DEFAULT 'USER' NOT NULL;
ALTER TABLE USERS ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

COMMIT;
