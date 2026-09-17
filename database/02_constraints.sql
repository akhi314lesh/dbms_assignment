-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 02_constraints.sql
-- Description: Defines Foreign Keys, Unique constraints, and Check constraints.
-- Note: Foreign keys strictly implement standard referential integrity as 
--       specified in the master schema (no unrequested ON DELETE CASCADE clauses).
-- ============================================================================

-- ============================================================================
-- 1. UNIQUE CONSTRAINTS
-- ============================================================================

-- USERS: Email candidate / alternate key
ALTER TABLE USERS 
    ADD CONSTRAINT uq_users_email UNIQUE (email);

-- ALERT_CATEGORIES: Unique category name
ALTER TABLE ALERT_CATEGORIES 
    ADD CONSTRAINT uq_alert_category_name UNIQUE (category_name);


-- ============================================================================
-- 2. FOREIGN KEY CONSTRAINTS (23 Total Foreign Keys)
-- ============================================================================

-- USER_CONTACT_NUMBERS -> USERS
ALTER TABLE USER_CONTACT_NUMBERS
    ADD CONSTRAINT fk_contact_user
    FOREIGN KEY (user_id) REFERENCES USERS(user_id);

-- HOME_ACCESS -> USERS
ALTER TABLE HOME_ACCESS
    ADD CONSTRAINT fk_home_access_user
    FOREIGN KEY (user_id) REFERENCES USERS(user_id);

-- HOME_ACCESS -> HOMES
ALTER TABLE HOME_ACCESS
    ADD CONSTRAINT fk_home_access_home
    FOREIGN KEY (home_id) REFERENCES HOMES(home_id);

-- ROOMS -> HOMES
ALTER TABLE ROOMS
    ADD CONSTRAINT fk_rooms_home
    FOREIGN KEY (home_id) REFERENCES HOMES(home_id);

-- DEVICES -> ROOMS
ALTER TABLE DEVICES
    ADD CONSTRAINT fk_devices_room
    FOREIGN KEY (room_id) REFERENCES ROOMS(room_id);

-- DEVICES -> DEVICES (Self-referencing unary Controls relationship)
-- Added via ALTER TABLE as specified in master schema
ALTER TABLE DEVICES 
    ADD CONSTRAINT fk_device_parent 
    FOREIGN KEY (parent_device_id) REFERENCES DEVICES(device_id);

-- SMART_LIGHTS -> DEVICES (Subtype specialization)
ALTER TABLE SMART_LIGHTS
    ADD CONSTRAINT fk_smart_lights_device
    FOREIGN KEY (device_id) REFERENCES DEVICES(device_id);

-- THERMOSTATS -> DEVICES (Subtype specialization)
ALTER TABLE THERMOSTATS
    ADD CONSTRAINT fk_thermostats_device
    FOREIGN KEY (device_id) REFERENCES DEVICES(device_id);

-- TEMPERATURE_SENSORS -> DEVICES (Subtype specialization)
ALTER TABLE TEMPERATURE_SENSORS
    ADD CONSTRAINT fk_temp_sensors_device
    FOREIGN KEY (device_id) REFERENCES DEVICES(device_id);

-- MOTION_SENSORS -> DEVICES (Subtype specialization)
ALTER TABLE MOTION_SENSORS
    ADD CONSTRAINT fk_motion_sensors_device
    FOREIGN KEY (device_id) REFERENCES DEVICES(device_id);

-- CAMERAS -> DEVICES (Subtype specialization)
ALTER TABLE CAMERAS
    ADD CONSTRAINT fk_cameras_device
    FOREIGN KEY (device_id) REFERENCES DEVICES(device_id);

-- SENSOR_READINGS -> DEVICES (Weak entity identifying relationship)
ALTER TABLE SENSOR_READINGS
    ADD CONSTRAINT fk_sensor_readings_device
    FOREIGN KEY (device_id) REFERENCES DEVICES(device_id);

-- AUTOMATION_RULES -> DEVICES (Condition source device)
ALTER TABLE AUTOMATION_RULES
    ADD CONSTRAINT fk_rules_condition_device
    FOREIGN KEY (condition_device_id) REFERENCES DEVICES(device_id);

-- AUTOMATION_RULES -> USERS (Rule creator)
ALTER TABLE AUTOMATION_RULES
    ADD CONSTRAINT fk_rules_created_by_user
    FOREIGN KEY (created_by_user_id) REFERENCES USERS(user_id);

-- RULE_ACTIONS -> AUTOMATION_RULES
ALTER TABLE RULE_ACTIONS
    ADD CONSTRAINT fk_rule_actions_rule
    FOREIGN KEY (rule_id) REFERENCES AUTOMATION_RULES(rule_id);

-- RULE_ACTIONS -> DEVICES (Target actuator device)
ALTER TABLE RULE_ACTIONS
    ADD CONSTRAINT fk_rule_actions_target_device
    FOREIGN KEY (target_device_id) REFERENCES DEVICES(device_id);

-- ALERTS -> DEVICES
ALTER TABLE ALERTS
    ADD CONSTRAINT fk_alerts_device
    FOREIGN KEY (device_id) REFERENCES DEVICES(device_id);

-- ALERTS -> ALERT_CATEGORIES (Schema refinement retained)
ALTER TABLE ALERTS
    ADD CONSTRAINT fk_alerts_category
    FOREIGN KEY (category_id) REFERENCES ALERT_CATEGORIES(category_id);

-- ALERTS -> AUTOMATION_RULES (Optional trigger rule)
ALTER TABLE ALERTS
    ADD CONSTRAINT fk_alerts_rule
    FOREIGN KEY (triggered_by_rule_id) REFERENCES AUTOMATION_RULES(rule_id);

-- ALERTS -> USERS (Optional user acknowledgement)
ALTER TABLE ALERTS
    ADD CONSTRAINT fk_alerts_acknowledged_by
    FOREIGN KEY (acknowledged_by) REFERENCES USERS(user_id);

-- NOTIFICATION_PREFERENCES -> USERS (Ternary component 1)
ALTER TABLE NOTIFICATION_PREFERENCES
    ADD CONSTRAINT fk_notif_pref_user
    FOREIGN KEY (user_id) REFERENCES USERS(user_id);

-- NOTIFICATION_PREFERENCES -> DEVICES (Ternary component 2)
ALTER TABLE NOTIFICATION_PREFERENCES
    ADD CONSTRAINT fk_notif_pref_device
    FOREIGN KEY (device_id) REFERENCES DEVICES(device_id);

-- NOTIFICATION_PREFERENCES -> ALERT_CATEGORIES (Ternary component 3)
ALTER TABLE NOTIFICATION_PREFERENCES
    ADD CONSTRAINT fk_notif_pref_category
    FOREIGN KEY (category_id) REFERENCES ALERT_CATEGORIES(category_id);


-- ============================================================================
-- 3. CHECK CONSTRAINTS (Explicit Master Schema Requirements Only)
-- ============================================================================

-- NOTIFICATION_PREFERENCES: enabled flag must be binary 0 or 1
-- Stated in Master Schema Section 17: CHECK (enabled IN (0,1))
ALTER TABLE NOTIFICATION_PREFERENCES
    ADD CONSTRAINT chk_notif_pref_enabled
    CHECK (enabled IN (0, 1));

-- DEVICES: Prevent a device from being its own parent
-- Stated in Master Schema Section 6 & Design Rules
ALTER TABLE DEVICES
    ADD CONSTRAINT chk_device_no_self_parent
    CHECK (parent_device_id IS NULL OR parent_device_id != device_id);

-- HOME_ACCESS: Enforce schema's intended home-level roles (OWNER / MEMBER)
-- Stated in Master Schema Section 4
ALTER TABLE HOME_ACCESS
    ADD CONSTRAINT chk_home_access_role
    CHECK (role IN ('OWNER', 'MEMBER'));

-- ALERTS: Valid alert source
-- Stated in Master Schema Section 16: source - 'RULE' or 'THRESHOLD'
ALTER TABLE ALERTS
    ADD CONSTRAINT chk_alert_source
    CHECK (source IN ('RULE', 'THRESHOLD'));
