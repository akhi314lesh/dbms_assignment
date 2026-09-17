-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 01_create_tables.sql
-- Description: Creates all 17 tables according to the finalized master schema.
-- Execution Order:
--   1. USERS
--   2. ALERT_CATEGORIES
--   3. HOMES
--   4. HOME_ACCESS
--   5. ROOMS
--   6. DEVICES
--   7. SMART_LIGHTS
--   8. THERMOSTATS
--   9. TEMPERATURE_SENSORS
--  10. MOTION_SENSORS
--  11. CAMERAS
--  12. SENSOR_READINGS
--  13. USER_CONTACT_NUMBERS
--  14. AUTOMATION_RULES
--  15. RULE_ACTIONS
--  16. ALERTS
--  17. NOTIFICATION_PREFERENCES
-- Note: Foreign key constraints, check constraints, and device self-referencing
--       keys are added in 02_constraints.sql.
-- ============================================================================

-- 1. USERS
-- System users who own or are granted access to homes.
CREATE TABLE USERS (
    user_id     NUMBER          NOT NULL,
    name        VARCHAR2(100)   NOT NULL,
    email       VARCHAR2(100)   NOT NULL,
    password    VARCHAR2(255)   NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

-- 2. ALERT_CATEGORIES
-- Taxonomical categorization of alerts (e.g. SECURITY, ENVIRONMENTAL, MAINTENANCE).
CREATE TABLE ALERT_CATEGORIES (
    category_id      NUMBER          NOT NULL,
    category_name    VARCHAR2(50)    NOT NULL,
    default_severity VARCHAR2(20)    NOT NULL,
    CONSTRAINT pk_alert_categories PRIMARY KEY (category_id)
);

-- 3. HOMES
-- Physical residential or commercial properties being monitored.
-- Note: No user_id column; access/ownership is M:N via HOME_ACCESS.
CREATE TABLE HOMES (
    home_id     NUMBER          NOT NULL,
    home_name   VARCHAR2(100)   NOT NULL,
    street      VARCHAR2(150),
    city        VARCHAR2(50),
    pincode     VARCHAR2(10),   -- Retained as VARCHAR2 to preserve leading zeros
    CONSTRAINT pk_homes PRIMARY KEY (home_id)
);

-- 4. HOME_ACCESS
-- Associative entity resolving M:N relationship between USERS and HOMES.
CREATE TABLE HOME_ACCESS (
    user_id      NUMBER         NOT NULL,
    home_id      NUMBER         NOT NULL,
    role         VARCHAR2(20)   NOT NULL, -- e.g. 'OWNER', 'MEMBER'
    date_granted DATE           NOT NULL,
    CONSTRAINT pk_home_access PRIMARY KEY (user_id, home_id)
);

-- 5. ROOMS
-- Partitions/locations within a home.
CREATE TABLE ROOMS (
    room_id      NUMBER          NOT NULL,
    room_name    VARCHAR2(100)   NOT NULL,
    floor_number NUMBER,
    home_id      NUMBER          NOT NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (room_id)
);

-- 6. DEVICES
-- Base entity for all smart IoT hardware deployed in rooms.
-- Note: parent_device_id implements unary/recursive Controls relationship.
-- Note: Uptime is derived, never stored (SYSTIMESTAMP - last_restart_time).
CREATE TABLE DEVICES (
    device_id          NUMBER          NOT NULL,
    device_name        VARCHAR2(100)   NOT NULL,
    device_subtype     VARCHAR2(50)    NOT NULL, -- Discriminator: LIGHT, THERMOSTAT, etc.
    status             VARCHAR2(20)    NOT NULL, -- e.g. 'ONLINE', 'OFFLINE', 'ERROR'
    install_date       DATE,
    last_restart_time  TIMESTAMP,
    room_id            NUMBER          NOT NULL,
    parent_device_id   NUMBER,                   -- Self-referencing FK added in 02_constraints.sql
    CONSTRAINT pk_devices PRIMARY KEY (device_id)
);

-- 7. SMART_LIGHTS
-- Subtype entity for controllable lights (specialization of DEVICES).
CREATE TABLE SMART_LIGHTS (
    device_id      NUMBER         NOT NULL,
    brightness     NUMBER(5,2),
    color_support  VARCHAR2(20),
    CONSTRAINT pk_smart_lights PRIMARY KEY (device_id)
);

-- 8. THERMOSTATS
-- Subtype entity for climate control devices (specialization of DEVICES).
CREATE TABLE THERMOSTATS (
    device_id          NUMBER         NOT NULL,
    target_temperature NUMBER(5,2),
    mode               VARCHAR2(30),  -- e.g. 'HEAT', 'COOL', 'ECO', 'OFF'
    CONSTRAINT pk_thermostats PRIMARY KEY (device_id)
);

-- 9. TEMPERATURE_SENSORS
-- Subtype entity for ambient temperature measuring devices (specialization of DEVICES).
CREATE TABLE TEMPERATURE_SENSORS (
    device_id  NUMBER         NOT NULL,
    unit       VARCHAR2(10),  -- e.g. 'CELSIUS', 'FAHRENHEIT'
    min_range  NUMBER(6,2),
    max_range  NUMBER(6,2),
    CONSTRAINT pk_temperature_sensors PRIMARY KEY (device_id)
);

-- 10. MOTION_SENSORS
-- Subtype entity for passive infrared/motion detection sensors (specialization of DEVICES).
CREATE TABLE MOTION_SENSORS (
    device_id         NUMBER         NOT NULL,
    sensitivity_level VARCHAR2(30),  -- e.g. 'LOW', 'MEDIUM', 'HIGH'
    detection_range   NUMBER(6,2),   -- In meters or feet
    CONSTRAINT pk_motion_sensors PRIMARY KEY (device_id)
);

-- 11. CAMERAS
-- Subtype entity for video surveillance and recording cameras (specialization of DEVICES).
CREATE TABLE CAMERAS (
    device_id             NUMBER         NOT NULL,
    resolution            VARCHAR2(30),  -- e.g. '1080P', '4K'
    storage_type          VARCHAR2(30),  -- e.g. 'LOCAL_SD', 'CLOUD'
    night_vision_support  VARCHAR2(20),  -- e.g. 'YES', 'NO', 'IR'
    CONSTRAINT pk_cameras PRIMARY KEY (device_id)
);

-- 12. SENSOR_READINGS
-- Weak entity dependent on DEVICES. Uses composite PK (device_id, reading_id).
CREATE TABLE SENSOR_READINGS (
    device_id    NUMBER        NOT NULL,
    reading_id   NUMBER        NOT NULL,
    value        NUMBER(10,2)  NOT NULL,
    reading_time TIMESTAMP     NOT NULL,
    CONSTRAINT pk_sensor_readings PRIMARY KEY (device_id, reading_id)
);

-- 13. USER_CONTACT_NUMBERS
-- Resolves the multi-valued ContactNumbers attribute of USERS.
CREATE TABLE USER_CONTACT_NUMBERS (
    user_id        NUMBER        NOT NULL,
    contact_number VARCHAR2(20)  NOT NULL,
    number_type    VARCHAR2(20), -- e.g. 'MOBILE', 'HOME', 'WORK'
    CONSTRAINT pk_user_contact_numbers PRIMARY KEY (user_id, contact_number)
);

-- 14. AUTOMATION_RULES
-- Logic rules triggered by sensor conditions or device states.
CREATE TABLE AUTOMATION_RULES (
    rule_id             NUMBER         NOT NULL,
    rule_name           VARCHAR2(100)  NOT NULL,
    condition_operator  VARCHAR2(10)   NOT NULL, -- e.g. '>', '<', '=', '>='
    condition_value     VARCHAR2(50)   NOT NULL,
    condition_device_id NUMBER         NOT NULL,
    created_by_user_id  NUMBER         NOT NULL,
    created_date        DATE           NOT NULL,
    CONSTRAINT pk_automation_rules PRIMARY KEY (rule_id)
);

-- 15. RULE_ACTIONS
-- Actions executed when an automation rule condition evaluates to TRUE.
CREATE TABLE RULE_ACTIONS (
    action_id        NUMBER         NOT NULL,
    rule_id          NUMBER         NOT NULL,
    target_device_id NUMBER         NOT NULL,
    action_type      VARCHAR2(50)   NOT NULL, -- e.g. 'TURN_ON', 'SET_TEMP', 'SEND_ALERT'
    action_value     VARCHAR2(100),
    CONSTRAINT pk_rule_actions PRIMARY KEY (action_id)
);

-- 16. ALERTS
-- Notifications generated by rules or threshold triggers.
-- Retains category_id as specified in the finalized master schema.
CREATE TABLE ALERTS (
    alert_id               NUMBER         NOT NULL,
    device_id              NUMBER         NOT NULL,
    category_id            NUMBER,
    triggered_by_rule_id   NUMBER,
    message                VARCHAR2(500),
    alert_time             TIMESTAMP      NOT NULL,
    source                 VARCHAR2(20)   NOT NULL, -- 'RULE' or 'THRESHOLD'
    status                 VARCHAR2(20)   NOT NULL, -- e.g. 'ACTIVE', 'ACKNOWLEDGED', 'CLEARED'
    acknowledged_by        NUMBER,
    acknowledged_timestamp TIMESTAMP,
    CONSTRAINT pk_alerts PRIMARY KEY (alert_id)
);

-- 17. NOTIFICATION_PREFERENCES
-- Associative entity for genuine ternary relationship (USER, DEVICE, ALERT_CATEGORY).
CREATE TABLE NOTIFICATION_PREFERENCES (
    user_id     NUMBER         NOT NULL,
    device_id   NUMBER         NOT NULL,
    category_id NUMBER         NOT NULL,
    channel     VARCHAR2(20)   NOT NULL, -- e.g. 'EMAIL', 'SMS', 'PUSH'
    enabled     NUMBER(1)      NOT NULL, -- Enforced via CHECK constraint in 02_constraints.sql
    CONSTRAINT pk_notification_preferences PRIMARY KEY (user_id, device_id, category_id)
);
