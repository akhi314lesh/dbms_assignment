-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 03_sequences.sql
-- Description: Creates Oracle sequences for surrogate primary keys.
-- ============================================================================

-- 1. SEQ_USER_ID
-- Generates surrogate primary key for USERS (user_id).
CREATE SEQUENCE SEQ_USER_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 2. SEQ_HOME_ID
-- Generates surrogate primary key for HOMES (home_id).
CREATE SEQUENCE SEQ_HOME_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 3. SEQ_ROOM_ID
-- Generates surrogate primary key for ROOMS (room_id).
CREATE SEQUENCE SEQ_ROOM_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 4. SEQ_DEVICE_ID
-- Generates surrogate primary key for DEVICES (device_id).
-- Also used as the 1:1 matching PK/FK in subtype tables:
-- SMART_LIGHTS, THERMOSTATS, TEMPERATURE_SENSORS, MOTION_SENSORS, and CAMERAS.
CREATE SEQUENCE SEQ_DEVICE_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 5. SEQ_READING_ID
-- Generates partial key reading_id for weak entity SENSOR_READINGS.
-- Logical primary key remains composite (device_id, reading_id).
CREATE SEQUENCE SEQ_READING_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 6. SEQ_RULE_ID
-- Generates surrogate primary key for AUTOMATION_RULES (rule_id).
CREATE SEQUENCE SEQ_RULE_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 7. SEQ_ACTION_ID
-- Generates surrogate primary key for RULE_ACTIONS (action_id).
CREATE SEQUENCE SEQ_ACTION_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 8. SEQ_CATEGORY_ID
-- Generates surrogate primary key for ALERT_CATEGORIES (category_id).
CREATE SEQUENCE SEQ_CATEGORY_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 9. SEQ_ALERT_ID
-- Generates surrogate primary key for ALERTS (alert_id).
CREATE SEQUENCE SEQ_ALERT_ID
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;
