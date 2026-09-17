-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 04_sample_data.sql
-- Description: Realistic test & demonstration data covering all 17 tables.
-- Note: Passwords are development mock hashes (BCrypt placeholders). Real
--       application security must hash passwords and never store plaintext.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. USERS
-- ----------------------------------------------------------------------------
INSERT INTO USERS (user_id, name, email, password)
VALUES (1, 'Alexander Wright', 'alex.wright@smarthome.io', '$2a$10$devPlaceholderHashForTestingAlexander01');

INSERT INTO USERS (user_id, name, email, password)
VALUES (2, 'Elena Rostova', 'elena.rostova@smarthome.io', '$2a$10$devPlaceholderHashForTestingElena000002');

INSERT INTO USERS (user_id, name, email, password)
VALUES (3, 'Marcus Vance', 'marcus.vance@smarthome.io', '$2a$10$devPlaceholderHashForTestingMarcus000003');

INSERT INTO USERS (user_id, name, email, password)
VALUES (4, 'Sophia Chen', 'sophia.chen@smarthome.io', '$2a$10$devPlaceholderHashForTestingSophia000004');


-- ----------------------------------------------------------------------------
-- 2. USER_CONTACT_NUMBERS
-- ----------------------------------------------------------------------------
INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (1, '+1-555-0101', 'MOBILE');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (1, '+1-555-0102', 'HOME');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (2, '+1-555-0201', 'MOBILE');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (3, '+1-555-0301', 'WORK');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (4, '+1-555-0401', 'MOBILE');


-- ----------------------------------------------------------------------------
-- 3. ALERT_CATEGORIES
-- ----------------------------------------------------------------------------
INSERT INTO ALERT_CATEGORIES (category_id, category_name, default_severity)
VALUES (1, 'SECURITY', 'HIGH');

INSERT INTO ALERT_CATEGORIES (category_id, category_name, default_severity)
VALUES (2, 'ENVIRONMENTAL', 'MEDIUM');

INSERT INTO ALERT_CATEGORIES (category_id, category_name, default_severity)
VALUES (3, 'MAINTENANCE', 'LOW');


-- ----------------------------------------------------------------------------
-- 4. HOMES
-- ----------------------------------------------------------------------------
INSERT INTO HOMES (home_id, home_name, street, city, pincode)
VALUES (1, 'Sanctuary Haven', '48B Meadowbrook Lane', 'San Jose', '95123');

INSERT INTO HOMES (home_id, home_name, street, city, pincode)
VALUES (2, 'Alpine Ridge Retreat', '742 Evergreen Terrace', 'Boulder', '80302');


-- ----------------------------------------------------------------------------
-- 5. HOME_ACCESS (Demonstrates M:N User <-> Home with OWNER and MEMBER roles)
-- ----------------------------------------------------------------------------
-- Alexander is OWNER of Home 1 and MEMBER of Home 2
INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (1, 1, 'OWNER', DATE '2026-01-10');

INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (1, 2, 'MEMBER', DATE '2026-03-01');

-- Elena is MEMBER of Home 1
INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (2, 1, 'MEMBER', DATE '2026-01-15');

-- Marcus is OWNER of Home 2 and MEMBER of Home 1
INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (3, 2, 'OWNER', DATE '2026-02-10');

INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (3, 1, 'MEMBER', DATE '2026-02-01');

-- Sophia is MEMBER of Home 1
INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (4, 1, 'MEMBER', DATE '2026-03-15');


-- ----------------------------------------------------------------------------
-- 6. ROOMS
-- ----------------------------------------------------------------------------
-- Home 1 Rooms
INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (1, 'Living Room', 1, 1);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (2, 'Master Bedroom', 2, 1);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (3, 'Kitchen', 1, 1);

-- Home 2 Rooms
INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (4, 'Great Room', 1, 2);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (5, 'Patio & Garden', 1, 2);


-- ----------------------------------------------------------------------------
-- 7. DEVICES
-- Demonstrates unary/recursive parent-child relationship (parent_device_id)
-- ----------------------------------------------------------------------------
-- Home 1 Central Hub (Parent Device)
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (1, 'Living Room Central Hub', 'HUB', 'ONLINE', DATE '2026-01-11', TIMESTAMP '2026-09-10 08:00:00', 1, NULL);

-- Home 1 Subordinate Devices (Controlled by Hub 1)
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (2, 'Living Room Chandelier Light', 'SMART_LIGHT', 'ONLINE', DATE '2026-01-12', TIMESTAMP '2026-09-12 10:30:00', 1, 1);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (3, 'Living Room Smart Thermostat', 'THERMOSTAT', 'ONLINE', DATE '2026-01-12', TIMESTAMP '2026-09-01 12:00:00', 1, 1);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (4, 'Kitchen Ambient Temp Sensor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-01-14', TIMESTAMP '2026-09-05 06:15:00', 3, 1);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (5, 'Master Bedroom Motion Sensor', 'MOTION_SENSOR', 'ONLINE', DATE '2026-01-15', TIMESTAMP '2026-09-14 22:00:00', 2, 1);

-- Home 1 Standalone Camera
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (6, 'Front Entrance Camera', 'CAMERA', 'ONLINE', DATE '2026-01-11', TIMESTAMP '2026-09-15 03:00:00', 1, NULL);

-- Home 2 Devices
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (7, 'Great Room Ambient Light', 'SMART_LIGHT', 'OFFLINE', DATE '2026-02-12', TIMESTAMP '2026-08-20 14:00:00', 4, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (8, 'Patio Weather Temp Sensor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-02-15', TIMESTAMP '2026-09-16 09:00:00', 5, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (9, 'Garden Perimeter Camera', 'CAMERA', 'ERROR', DATE '2026-02-16', TIMESTAMP '2026-09-17 01:20:00', 5, NULL);


-- ----------------------------------------------------------------------------
-- 8. DEVICE SUBTYPE TABLES (Specialization records)
-- ----------------------------------------------------------------------------
-- SMART_LIGHTS
INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (2, 80.00, 'RGB');

INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (7, 50.00, 'WARM_WHITE');

-- THERMOSTATS ("MODE" quoted because MODE is an Oracle reserved word)
INSERT INTO THERMOSTATS (device_id, target_temperature, "MODE")
VALUES (3, 22.50, 'COOL');

-- TEMPERATURE_SENSORS
INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (4, 'CELSIUS', -10.00, 60.00);

INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (8, 'CELSIUS', -25.00, 55.00);

-- MOTION_SENSORS
INSERT INTO MOTION_SENSORS (device_id, sensitivity_level, detection_range)
VALUES (5, 'HIGH', 8.50);

-- CAMERAS
INSERT INTO CAMERAS (device_id, resolution, storage_type, night_vision_support)
VALUES (6, '4K', 'CLOUD', 'YES');

INSERT INTO CAMERAS (device_id, resolution, storage_type, night_vision_support)
VALUES (9, '1080P', 'LOCAL_SD', 'IR');


-- ----------------------------------------------------------------------------
-- 9. SENSOR_READINGS (Weak Entity: composite PK device_id + reading_id)
-- ----------------------------------------------------------------------------
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time)
VALUES (4, 1, 23.40, TIMESTAMP '2026-09-17 18:00:00');

INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time)
VALUES (4, 2, 24.10, TIMESTAMP '2026-09-17 19:00:00');

INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time)
VALUES (4, 3, 26.80, TIMESTAMP '2026-09-17 20:00:00');

INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time)
VALUES (8, 4, 14.20, TIMESTAMP '2026-09-17 18:00:00');

INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time)
VALUES (8, 5, 12.80, TIMESTAMP '2026-09-17 20:00:00');


-- ----------------------------------------------------------------------------
-- 10. AUTOMATION_RULES
-- ----------------------------------------------------------------------------
-- Rule 1: When Kitchen Temp > 25.0 C, activate cooling
INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (1, 'Kitchen High Temperature Cool Down', '>', '25.0', 4, 1, DATE '2026-02-01');

-- Rule 2: When Bedroom Motion detected, activate security protocol
INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (2, 'Master Bedroom Night Security Alert', '=', 'ACTIVE', 5, 1, DATE '2026-02-15');


-- ----------------------------------------------------------------------------
-- 11. RULE_ACTIONS (Multiple actions driven by a single rule)
-- ----------------------------------------------------------------------------
-- Rule 1 drives both adjusting the thermostat and dimming the light to blue
INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (1, 1, 3, 'SET_TEMP', '21.0');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (2, 1, 2, 'SET_BRIGHTNESS', '60');

-- Rule 2 activates front entrance camera recording
INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (3, 2, 6, 'RECORD_CLIP', '60S');


-- ----------------------------------------------------------------------------
-- 12. ALERTS (Acknowledged and unacknowledged alerts with category_id retained)
-- ----------------------------------------------------------------------------
-- Alert 1: Acknowledged environmental alert triggered by Rule 1
INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (
    1, 4, 2, 1, 
    'Kitchen ambient temperature exceeded threshold: 26.8C', 
    TIMESTAMP '2026-09-17 20:00:05', 'RULE', 'ACKNOWLEDGED', 
    1, TIMESTAMP '2026-09-17 20:10:00'
);

-- Alert 2: Unacknowledged security alert triggered by Rule 2
INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (
    2, 5, 1, 2, 
    'Unscheduled motion detected in Master Bedroom', 
    TIMESTAMP '2026-09-17 21:05:00', 'RULE', 'ACTIVE', 
    NULL, NULL
);

-- Alert 3: Unacknowledged hardware maintenance alert triggered by threshold/disconnection
INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (
    3, 9, 3, NULL, 
    'Garden Perimeter Camera offline: storage communication failure', 
    TIMESTAMP '2026-09-17 01:20:00', 'THRESHOLD', 'ACTIVE', 
    NULL, NULL
);


-- ----------------------------------------------------------------------------
-- 13. NOTIFICATION_PREFERENCES (Genuine ternary relationship M:N:M)
-- ----------------------------------------------------------------------------
-- Alexander wants PUSH notifications for Kitchen Temp Sensor environmental alerts
INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (1, 4, 2, 'PUSH', 1);

-- Alexander wants PUSH notifications for Front Camera security alerts
INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (1, 6, 1, 'PUSH', 1);

-- Elena wants SMS notifications for Bedroom Motion security alerts
INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (2, 5, 1, 'SMS', 1);

-- Marcus wants EMAIL notifications for Garden Camera maintenance alerts
INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (3, 9, 3, 'EMAIL', 1);

-- Sophia has disabled PUSH notifications for Front Camera security alerts
INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (4, 6, 1, 'PUSH', 0);

COMMIT;
