-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Script: 00_create_user.sql   (run ONCE as SYSTEM, connected to XEPDB1)
--
--   sqlplus system/<your-system-password>@localhost:1521/XEPDB1 @00_create_user.sql
--
-- Creates the SMART_HOME schema the backend connects to. Change the password
-- here if you like, and use the same value for DB_PASSWORD when starting the
-- backend (default used by scripts/start-local.ps1: SmartHomeDev123).
-- Skip this script if your SMART_HOME user already exists.
-- ============================================================================
CREATE USER SMART_HOME IDENTIFIED BY SmartHomeDev123
    DEFAULT TABLESPACE USERS
    QUOTA UNLIMITED ON USERS;

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE PROCEDURE,
      CREATE TRIGGER, CREATE VIEW TO SMART_HOME;
