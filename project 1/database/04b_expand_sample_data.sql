-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 04b_expand_sample_data.sql
-- Description: Incremental delta expansion script. Inserts ONLY new records
--              to grow the existing Oracle dataset to the full target totals.
--              Safely executes against a database that already contains the
--              initial sample data without primary key collisions.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. USERS (Inserts 4 new users: IDs 5, 6, 7, 8 -> Total: 8)
-- ----------------------------------------------------------------------------
INSERT INTO USERS (user_id, name, email, password)
VALUES (5, 'David Miller', 'david.miller@smarthome.io', '$2a$10$devPlaceholderHashForTestingDavid0000005');

INSERT INTO USERS (user_id, name, email, password)
VALUES (6, 'Amina Al-Mansoor', 'amina.mansoor@smarthome.io', '$2a$10$devPlaceholderHashForTestingAmina0000006');

INSERT INTO USERS (user_id, name, email, password)
VALUES (7, 'Liam O Connor', 'liam.oconnor@smarthome.io', '$2a$10$devPlaceholderHashForTestingLiam00000007');

INSERT INTO USERS (user_id, name, email, password)
VALUES (8, 'Zara Patel', 'zara.patel@smarthome.io', '$2a$10$devPlaceholderHashForTestingZara000000008');


-- ----------------------------------------------------------------------------
-- 2. USER_CONTACT_NUMBERS (Inserts 9 new numbers for users 5-8 -> Total: 14)
-- ----------------------------------------------------------------------------
INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (5, '+1-555-0501', 'MOBILE');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (5, '+1-555-0502', 'WORK');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (6, '+1-555-0601', 'MOBILE');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (6, '+1-555-0602', 'HOME');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (7, '+1-555-0701', 'MOBILE');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (7, '+1-555-0702', 'WORK');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (8, '+1-555-0801', 'MOBILE');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (8, '+1-555-0802', 'HOME');

INSERT INTO USER_CONTACT_NUMBERS (user_id, contact_number, number_type)
VALUES (8, '+1-555-0803', 'WORK');


-- ----------------------------------------------------------------------------
-- 3. ALERT_CATEGORIES (Inserts 2 new categories: IDs 4, 5 -> Total: 5)
-- ----------------------------------------------------------------------------
INSERT INTO ALERT_CATEGORIES (category_id, category_name, default_severity)
VALUES (4, 'ENERGY_MANAGEMENT', 'LOW');

INSERT INTO ALERT_CATEGORIES (category_id, category_name, default_severity)
VALUES (5, 'HEALTH_SAFETY', 'HIGH');


-- ----------------------------------------------------------------------------
-- 4. HOMES (Inserts 3 new properties: IDs 3, 4, 5 -> Total: 5)
-- ----------------------------------------------------------------------------
INSERT INTO HOMES (home_id, home_name, street, city, pincode)
VALUES (3, 'Coastal Horizon Villa', '108 Pacific Coast Highway', 'Malibu', '90265');

INSERT INTO HOMES (home_id, home_name, street, city, pincode)
VALUES (4, 'Skyline Urban Loft', '550 North Michigan Avenue', 'Chicago', '60611');

INSERT INTO HOMES (home_id, home_name, street, city, pincode)
VALUES (5, 'Cedar Grove Residence', '320 Whispering Pines Trail', 'Seattle', '98101');


-- ----------------------------------------------------------------------------
-- 5. HOME_ACCESS (Inserts 8 new M:N access links -> Total: 14)
-- ----------------------------------------------------------------------------
-- Home 3 Access
INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (5, 3, 'OWNER', DATE '2026-02-01');

INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (6, 3, 'MEMBER', DATE '2026-02-15');

INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (8, 3, 'MEMBER', DATE '2026-03-12');

-- Home 4 Access
INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (6, 4, 'OWNER', DATE '2026-03-01');

INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (7, 4, 'MEMBER', DATE '2026-03-10');

INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (2, 4, 'MEMBER', DATE '2026-03-20');

-- Home 5 Access
INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (7, 5, 'OWNER', DATE '2026-01-20');

INSERT INTO HOME_ACCESS (user_id, home_id, role, date_granted)
VALUES (8, 5, 'MEMBER', DATE '2026-02-05');


-- ----------------------------------------------------------------------------
-- 6. ROOMS (Inserts 17 new rooms: IDs 6 to 22 -> Total: 22)
-- ----------------------------------------------------------------------------
-- Home 1 additional rooms
INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (6, 'Home Office', 2, 1);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (7, 'Basement Utility Room', 0, 1);

-- Home 2 additional rooms
INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (8, 'Guest Bedroom', 2, 2);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (9, 'Attic Storage', 3, 2);

-- Home 3 rooms
INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (10, 'Ocean View Living Area', 1, 3);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (11, 'Master Suite', 2, 3);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (12, 'Gourmet Kitchen', 1, 3);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (13, 'Sun Deck and Poolside', 1, 3);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (14, 'Wine Cellar', 0, 3);

-- Home 4 rooms
INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (15, 'Open Concept Lounge', 14, 4);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (16, 'Master Studio Bedroom', 14, 4);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (17, 'Balcony Terrace', 14, 4);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (18, 'Server and Media Closet', 14, 4);

-- Home 5 rooms
INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (19, 'Family Gathering Room', 1, 5);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (20, 'Cedar Kitchen and Dining', 1, 5);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (21, 'Workshop and Garage', 1, 5);

INSERT INTO ROOMS (room_id, room_name, floor_number, home_id)
VALUES (22, 'Primary Bedroom Suite', 2, 5);


-- ----------------------------------------------------------------------------
-- 7. DEVICES (Inserts 36 new devices: IDs 10 to 45 -> Total: 45)
-- ----------------------------------------------------------------------------
-- Home 1 additional devices
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (12, 'Home Office Desk Light', 'SMART_LIGHT', 'ONLINE', DATE '2026-01-18', TIMESTAMP '2026-09-13 07:45:00', 6, 1);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (13, 'Office Temperature Monitor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-01-18', TIMESTAMP '2026-09-10 09:00:00', 6, 1);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (14, 'Basement Water and Temp Sensor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-01-20', TIMESTAMP '2026-09-08 11:30:00', 7, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (15, 'Basement Security Camera', 'CAMERA', 'OFFLINE', DATE '2026-01-20', TIMESTAMP '2026-08-30 18:00:00', 7, NULL);

-- Home 2 additional devices (Gateway Hub: 10)
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (10, 'Alpine Gateway Hub', 'HUB', 'ONLINE', DATE '2026-02-10', TIMESTAMP '2026-09-01 06:00:00', 4, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (11, 'Great Room Motion Sensor', 'MOTION_SENSOR', 'ONLINE', DATE '2026-02-14', TIMESTAMP '2026-09-15 14:00:00', 4, 10);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (26, 'Alpine Climate Thermostat', 'THERMOSTAT', 'ONLINE', DATE '2026-02-12', TIMESTAMP '2026-09-11 08:00:00', 4, 10);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (27, 'Attic Ambient Temp Sensor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-02-18', TIMESTAMP '2026-09-12 12:00:00', 9, NULL);

-- Home 3 devices (Core Hub: 16)
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (16, 'Malibu Automation Core Hub', 'HUB', 'ONLINE', DATE '2026-02-02', TIMESTAMP '2026-09-14 05:00:00', 10, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (17, 'Living Area Cove Light', 'SMART_LIGHT', 'ONLINE', DATE '2026-02-03', TIMESTAMP '2026-09-15 11:00:00', 10, 16);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (18, 'Malibu HVAC Thermostat', 'THERMOSTAT', 'ONLINE', DATE '2026-02-03', TIMESTAMP '2026-09-14 09:30:00', 10, 16);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (19, 'Kitchen Climate Temp Sensor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-02-04', TIMESTAMP '2026-09-16 10:15:00', 12, 16);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (20, 'Poolside Deck Motion Sensor', 'MOTION_SENSOR', 'ONLINE', DATE '2026-02-05', TIMESTAMP '2026-09-17 02:00:00', 13, 16);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (21, 'Poolside Security Camera', 'CAMERA', 'ONLINE', DATE '2026-02-05', TIMESTAMP '2026-09-15 04:00:00', 13, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (22, 'Master Suite Pendant Light', 'SMART_LIGHT', 'ONLINE', DATE '2026-02-06', TIMESTAMP '2026-09-16 20:00:00', 11, 16);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (23, 'Wine Cellar Temp Sensor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-02-08', TIMESTAMP '2026-09-17 07:00:00', 14, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (24, 'Master Suite Motion Detector', 'MOTION_SENSOR', 'ONLINE', DATE '2026-02-08', TIMESTAMP '2026-09-16 23:10:00', 11, 16);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (25, 'Wine Cellar Motion Detector', 'MOTION_SENSOR', 'ONLINE', DATE '2026-02-09', TIMESTAMP '2026-09-14 16:30:00', 14, NULL);

-- Home 4 devices (Smart Hub: 28)
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (28, 'Chicago Loft Smart Hub', 'HUB', 'ONLINE', DATE '2026-03-02', TIMESTAMP '2026-09-15 10:00:00', 15, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (29, 'Lounge Architectural Lighting', 'SMART_LIGHT', 'ONLINE', DATE '2026-03-03', TIMESTAMP '2026-09-15 12:00:00', 15, 28);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (30, 'Loft Eco Thermostat', 'THERMOSTAT', 'ONLINE', DATE '2026-03-03', TIMESTAMP '2026-09-15 10:30:00', 15, 28);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (31, 'Terrace Motion Sensor', 'MOTION_SENSOR', 'ONLINE', DATE '2026-03-04', TIMESTAMP '2026-09-16 21:00:00', 17, 28);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (32, 'Balcony Surveillance Camera', 'CAMERA', 'ONLINE', DATE '2026-03-04', TIMESTAMP '2026-09-16 02:00:00', 17, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (33, 'Server Room Thermal Sensor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-03-05', TIMESTAMP '2026-09-17 08:00:00', 18, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (34, 'Studio Bedroom Reading Light', 'SMART_LIGHT', 'ONLINE', DATE '2026-03-05', TIMESTAMP '2026-09-16 22:30:00', 16, 28);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (35, 'Studio Bedroom Temp Monitor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-03-06', TIMESTAMP '2026-09-17 06:45:00', 16, 28);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (36, 'Server Closet Door Camera', 'CAMERA', 'ONLINE', DATE '2026-03-06', TIMESTAMP '2026-09-15 15:00:00', 18, NULL);

-- Home 5 devices (Master Hub: 37)
INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (37, 'Seattle Woods Master Hub', 'HUB', 'ONLINE', DATE '2026-01-22', TIMESTAMP '2026-09-12 04:00:00', 19, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (38, 'Family Room Main Fixture', 'SMART_LIGHT', 'ONLINE', DATE '2026-01-23', TIMESTAMP '2026-09-16 18:00:00', 19, 37);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (39, 'Cedar Residence Thermostat', 'THERMOSTAT', 'ONLINE', DATE '2026-01-23', TIMESTAMP '2026-09-10 14:00:00', 19, 37);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (40, 'Workshop Ambient Temp Sensor', 'TEMPERATURE_SENSOR', 'ONLINE', DATE '2026-01-24', TIMESTAMP '2026-09-15 08:30:00', 21, 37);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (41, 'Workshop Intrusion Motion Sensor', 'MOTION_SENSOR', 'ONLINE', DATE '2026-01-25', TIMESTAMP '2026-09-17 03:30:00', 21, 37);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (42, 'Driveway Entrance Camera', 'CAMERA', 'ONLINE', DATE '2026-01-25', TIMESTAMP '2026-09-14 01:00:00', 21, NULL);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (43, 'Primary Suite Recessed Lights', 'SMART_LIGHT', 'ONLINE', DATE '2026-01-26', TIMESTAMP '2026-09-16 21:15:00', 22, 37);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (44, 'Dining Accent Strip Light', 'SMART_LIGHT', 'ONLINE', DATE '2026-01-26', TIMESTAMP '2026-09-15 19:45:00', 20, 37);

INSERT INTO DEVICES (device_id, device_name, device_subtype, status, install_date, last_restart_time, room_id, parent_device_id)
VALUES (45, 'Cedar Dining Motion Sensor', 'MOTION_SENSOR', 'ONLINE', DATE '2026-01-27', TIMESTAMP '2026-09-16 19:00:00', 20, 37);


-- ----------------------------------------------------------------------------
-- 8. SUBTYPE TABLES
-- ----------------------------------------------------------------------------

-- SMART_LIGHTS (Inserts 8 new records -> Total: 10)
INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (12, 75.00, 'COOL_WHITE');

INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (17, 90.00, 'RGBW');

INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (22, 40.00, 'AMBER');

INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (29, 85.00, 'RGBW');

INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (34, 60.00, 'WARM_WHITE');

INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (38, 70.00, 'TUNABLE_WHITE');

INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (43, 45.00, 'WARM_WHITE');

INSERT INTO SMART_LIGHTS (device_id, brightness, color_support)
VALUES (44, 80.00, 'RGB');

-- THERMOSTATS (Inserts 4 new records, "MODE" quoted -> Total: 5)
INSERT INTO THERMOSTATS (device_id, target_temperature, "MODE")
VALUES (18, 21.00, 'AUTO');

INSERT INTO THERMOSTATS (device_id, target_temperature, "MODE")
VALUES (26, 19.50, 'HEAT');

INSERT INTO THERMOSTATS (device_id, target_temperature, "MODE")
VALUES (30, 23.00, 'ECO');

INSERT INTO THERMOSTATS (device_id, target_temperature, "MODE")
VALUES (39, 20.00, 'HEAT');

-- TEMPERATURE_SENSORS (Inserts 8 new records -> Total: 10)
INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (13, 'CELSIUS', 0.00, 50.00);

INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (14, 'CELSIUS', -5.00, 65.00);

INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (19, 'FAHRENHEIT', 14.00, 140.00);

INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (23, 'CELSIUS', 5.00, 30.00);

INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (27, 'CELSIUS', -20.00, 70.00);

INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (33, 'CELSIUS', 10.00, 85.00);

INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (35, 'CELSIUS', 0.00, 50.00);

INSERT INTO TEMPERATURE_SENSORS (device_id, unit, min_range, max_range)
VALUES (40, 'CELSIUS', -15.00, 60.00);

-- MOTION_SENSORS (Inserts 7 new records -> Total: 8)
INSERT INTO MOTION_SENSORS (device_id, sensitivity_level, detection_range)
VALUES (11, 'MEDIUM', 6.00);

INSERT INTO MOTION_SENSORS (device_id, sensitivity_level, detection_range)
VALUES (20, 'HIGH', 12.00);

INSERT INTO MOTION_SENSORS (device_id, sensitivity_level, detection_range)
VALUES (24, 'LOW', 5.00);

INSERT INTO MOTION_SENSORS (device_id, sensitivity_level, detection_range)
VALUES (25, 'HIGH', 7.00);

INSERT INTO MOTION_SENSORS (device_id, sensitivity_level, detection_range)
VALUES (31, 'MEDIUM', 8.00);

INSERT INTO MOTION_SENSORS (device_id, sensitivity_level, detection_range)
VALUES (41, 'HIGH', 10.00);

INSERT INTO MOTION_SENSORS (device_id, sensitivity_level, detection_range)
VALUES (45, 'LOW', 4.50);

-- CAMERAS (Inserts 5 new records -> Total: 7)
INSERT INTO CAMERAS (device_id, resolution, storage_type, night_vision_support)
VALUES (15, '1080P', 'CLOUD', 'IR');

INSERT INTO CAMERAS (device_id, resolution, storage_type, night_vision_support)
VALUES (21, '4K', 'HYBRID', 'COLOR_NIGHT');

INSERT INTO CAMERAS (device_id, resolution, storage_type, night_vision_support)
VALUES (32, '2K', 'CLOUD', 'YES');

INSERT INTO CAMERAS (device_id, resolution, storage_type, night_vision_support)
VALUES (36, '1080P', 'LOCAL_SD', 'IR');

INSERT INTO CAMERAS (device_id, resolution, storage_type, night_vision_support)
VALUES (42, '4K', 'HYBRID', 'COLOR_NIGHT');


-- ----------------------------------------------------------------------------
-- 9. SENSOR_READINGS (Inserts 115 new readings: IDs 6 to 120 -> Total: 120)
-- ----------------------------------------------------------------------------
-- Additional readings for Device 4 (Readings 6 - 12)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (4, 6, 26.80, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (4, 7, 26.10, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (4, 8, 25.40, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (4, 9, 24.90, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (4, 10, 24.30, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (4, 11, 23.90, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (4, 12, 23.50, TIMESTAMP '2026-09-17 19:00:00');

-- Additional readings for Device 8 (Readings 13 - 24)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 13, 11.50, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 14, 12.80, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 15, 14.20, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 16, 15.60, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 17, 16.90, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 18, 17.50, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 19, 17.10, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 20, 16.20, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 21, 15.00, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 22, 13.80, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 23, 12.50, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (8, 24, 11.80, TIMESTAMP '2026-09-17 19:00:00');

-- Device 13: Office Temp Monitor (Readings 25 - 36)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 25, 20.80, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 26, 21.10, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 27, 21.50, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 28, 21.80, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 29, 22.20, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 30, 22.40, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 31, 22.10, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 32, 21.90, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 33, 21.60, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 34, 21.30, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 35, 21.00, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (13, 36, 20.70, TIMESTAMP '2026-09-17 19:00:00');

-- Device 14: Basement Temp Sensor (Readings 37 - 48)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 37, 18.00, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 38, 18.10, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 39, 18.10, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 40, 18.20, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 41, 18.30, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 42, 18.30, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 43, 18.20, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 44, 18.10, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 45, 18.00, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 46, 17.90, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 47, 18.00, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (14, 48, 18.00, TIMESTAMP '2026-09-17 19:00:00');

-- Device 19: Malibu Kitchen Climate Sensor (Fahrenheit) (Readings 49 - 60)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 49, 70.50, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 50, 71.20, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 51, 72.80, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 52, 73.90, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 53, 75.20, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 54, 76.50, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 55, 76.10, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 56, 75.00, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 57, 73.80, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 58, 72.40, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 59, 71.60, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (19, 60, 71.00, TIMESTAMP '2026-09-17 19:00:00');

-- Device 23: Wine Cellar Temp Sensor (Readings 61 - 72)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 61, 13.00, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 62, 13.10, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 63, 13.10, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 64, 13.20, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 65, 13.40, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 66, 14.50, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 67, 14.10, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 68, 13.60, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 69, 13.20, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 70, 13.10, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 71, 13.00, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (23, 72, 13.00, TIMESTAMP '2026-09-17 19:00:00');

-- Device 27: Attic Temp Sensor (Readings 73 - 84)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 73, 18.50, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 74, 21.00, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 75, 24.50, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 76, 28.00, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 77, 31.50, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 78, 33.80, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 79, 32.40, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 80, 29.80, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 81, 26.50, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 82, 23.20, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 83, 20.50, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (27, 84, 19.20, TIMESTAMP '2026-09-17 19:00:00');

-- Device 33: Chicago Server Thermal Sensor (Readings 85 - 96)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 85, 21.80, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 86, 22.00, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 87, 22.40, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 88, 23.10, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 89, 24.50, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 90, 26.90, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 91, 26.20, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 92, 24.80, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 93, 23.50, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 94, 22.90, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 95, 22.30, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (33, 96, 22.00, TIMESTAMP '2026-09-17 19:00:00');

-- Device 35: Studio Bedroom Temp Monitor (Readings 97 - 108)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 97, 20.20, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 98, 20.60, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 99, 21.00, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 100, 21.50, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 101, 22.00, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 102, 22.50, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 103, 22.30, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 104, 21.90, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 105, 21.40, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 106, 21.00, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 107, 20.70, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (35, 108, 20.40, TIMESTAMP '2026-09-17 19:00:00');

-- Device 40: Seattle Workshop Temp Sensor (Readings 109 - 120)
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 109, 15.50, TIMESTAMP '2026-09-17 08:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 110, 15.90, TIMESTAMP '2026-09-17 09:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 111, 16.50, TIMESTAMP '2026-09-17 10:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 112, 17.20, TIMESTAMP '2026-09-17 11:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 113, 17.90, TIMESTAMP '2026-09-17 12:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 114, 18.40, TIMESTAMP '2026-09-17 13:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 115, 18.00, TIMESTAMP '2026-09-17 14:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 116, 17.50, TIMESTAMP '2026-09-17 15:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 117, 16.80, TIMESTAMP '2026-09-17 16:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 118, 16.20, TIMESTAMP '2026-09-17 17:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 119, 15.80, TIMESTAMP '2026-09-17 18:00:00');
INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time) VALUES (40, 120, 15.40, TIMESTAMP '2026-09-17 19:00:00');


-- ----------------------------------------------------------------------------
-- 10. AUTOMATION_RULES (Inserts 13 new rules: IDs 3 to 15 -> Total: 15)
-- ----------------------------------------------------------------------------
INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (3, 'Basement Low Temperature Warning', '<', '5.0', 14, 1, DATE '2026-02-20');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (4, 'Alpine Great Room Presence Lighting', '=', 'ACTIVE', 11, 3, DATE '2026-02-25');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (5, 'Alpine Exterior Freeze Alert', '<', '0.0', 8, 3, DATE '2026-03-01');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (6, 'Attic Overheat Ventilation Trigger', '>', '30.0', 27, 3, DATE '2026-03-05');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (7, 'Poolside Intrusion Night Alert', '=', 'ACTIVE', 20, 5, DATE '2026-02-10');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (8, 'Wine Cellar Temperature Guard', '>', '14.0', 23, 5, DATE '2026-02-12');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (9, 'Master Suite Evening Walkway Light', '=', 'ACTIVE', 24, 5, DATE '2026-02-14');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (10, 'Malibu Kitchen Climate Threshold', '>', '75.0', 19, 5, DATE '2026-02-18');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (11, 'Chicago Server Thermal Critical Alert', '>', '25.0', 33, 6, DATE '2026-03-08');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (12, 'Chicago Terrace Perimeter Detection', '=', 'ACTIVE', 31, 6, DATE '2026-03-10');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (13, 'Seattle Workshop Off-Hours Intrusion', '=', 'ACTIVE', 41, 7, DATE '2026-02-01');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (14, 'Seattle Workshop Freeze Pre-Heat', '<', '10.0', 40, 7, DATE '2026-02-05');

INSERT INTO AUTOMATION_RULES (rule_id, rule_name, condition_operator, condition_value, condition_device_id, created_by_user_id, created_date)
VALUES (15, 'Cedar Dining Evening Presence Lighting', '=', 'ACTIVE', 45, 7, DATE '2026-02-10');


-- ----------------------------------------------------------------------------
-- 11. RULE_ACTIONS (Inserts 22 new actions: IDs 4 to 25 -> Total: 25)
-- ----------------------------------------------------------------------------
INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (4, 3, 3, 'SET_MODE', 'HEAT');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (5, 4, 7, 'TURN_ON', '100');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (6, 4, 26, 'SET_MODE', 'HEAT');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (7, 5, 9, 'TRIGGER_SNAPSHOT', 'NOW');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (8, 6, 26, 'SET_TEMP', '18.0');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (9, 7, 21, 'RECORD_CLIP', '120S');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (10, 7, 17, 'FLASH_ALERT', 'RED');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (11, 8, 18, 'SET_TEMP', '12.0');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (24, 8, 21, 'LOG_EVENT', 'WINE_CELLAR_WARN');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (12, 9, 22, 'SET_BRIGHTNESS', '25');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (13, 10, 18, 'SET_MODE', 'COOL');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (14, 10, 17, 'SET_COLOR', 'BLUE');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (15, 11, 30, 'SET_TEMP', '19.0');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (16, 11, 36, 'RECORD_CLIP', '300S');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (25, 11, 29, 'SET_COLOR', 'YELLOW');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (17, 12, 32, 'RECORD_CLIP', '60S');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (18, 12, 29, 'TURN_ON', '100');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (19, 13, 42, 'RECORD_CLIP', '90S');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (20, 13, 38, 'FLASH_ALERT', 'RED');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (21, 14, 39, 'SET_TEMP', '20.0');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (22, 15, 44, 'SET_BRIGHTNESS', '80');

INSERT INTO RULE_ACTIONS (action_id, rule_id, target_device_id, action_type, action_value)
VALUES (23, 1, 12, 'SET_COLOR', 'GREEN');


-- ----------------------------------------------------------------------------
-- 12. ALERTS (Inserts 32 new alerts: IDs 4 to 35 -> Total: 35)
-- ----------------------------------------------------------------------------
INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (4, 14, 2, 3, 'Basement temperature dropped near freeze warning limit: 4.8C', TIMESTAMP '2026-09-17 04:30:00', 'RULE', 'ACKNOWLEDGED', 1, TIMESTAMP '2026-09-17 05:00:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (5, 8, 2, 5, 'Alpine patio sensor detected sub-zero ambient temperature: -1.5C', TIMESTAMP '2026-09-17 05:15:00', 'RULE', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (6, 27, 2, 6, 'Attic temperature exceeded ventilation threshold: 33.8C', TIMESTAMP '2026-09-17 13:05:00', 'RULE', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (7, 15, 3, NULL, 'Basement Security Camera offline for over 48 hours', TIMESTAMP '2026-09-16 10:00:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (8, 7, 3, NULL, 'Great Room Ambient Light heartbeat timeout', TIMESTAMP '2026-09-15 14:00:00', 'THRESHOLD', 'ACKNOWLEDGED', 3, TIMESTAMP '2026-09-15 14:30:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (9, 23, 2, 8, 'Wine Cellar ambient temperature exceeded ideal preservation: 14.5C', TIMESTAMP '2026-09-17 13:02:00', 'RULE', 'ACKNOWLEDGED', 5, TIMESTAMP '2026-09-17 13:15:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (10, 19, 2, 10, 'Malibu gourmet kitchen high heat detected: 76.5F', TIMESTAMP '2026-09-17 13:00:00', 'RULE', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (11, 20, 1, 7, 'Poolside motion detected during armed night perimeter schedule', TIMESTAMP '2026-09-17 02:00:15', 'RULE', 'ACKNOWLEDGED', 5, TIMESTAMP '2026-09-17 02:05:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (12, 33, 2, 11, 'Chicago Server Closet exceeded thermal safety envelope: 26.9C', TIMESTAMP '2026-09-17 13:01:00', 'RULE', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (13, 31, 1, 12, 'Balcony terrace movement detected while residence vacant', TIMESTAMP '2026-09-17 03:45:00', 'RULE', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (14, 41, 1, 13, 'Unauthorized off-hours motion detected inside Seattle workshop', TIMESTAMP '2026-09-17 03:30:10', 'RULE', 'ACKNOWLEDGED', 7, TIMESTAMP '2026-09-17 03:35:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (15, 40, 2, 14, 'Workshop temperature dropped below working threshold: 9.8C', TIMESTAMP '2026-09-17 06:10:00', 'RULE', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (16, 3, 4, NULL, 'Thermostat continuous compressor cycle exceeded 4 hours', TIMESTAMP '2026-09-17 14:00:00', 'THRESHOLD', 'ACKNOWLEDGED', 1, TIMESTAMP '2026-09-17 14:20:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (17, 30, 4, NULL, 'Loft Eco Thermostat energy saving target reached for month', TIMESTAMP '2026-09-16 18:00:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (18, 6, 1, NULL, 'Entrance camera facial recognition: Unknown individual detected at gate', TIMESTAMP '2026-09-17 11:30:00', 'THRESHOLD', 'ACKNOWLEDGED', 2, TIMESTAMP '2026-09-17 11:35:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (19, 21, 5, NULL, 'Pool water perimeter motion detected: Child safety check reminder', TIMESTAMP '2026-09-17 16:15:00', 'THRESHOLD', 'ACKNOWLEDGED', 5, TIMESTAMP '2026-09-17 16:18:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (20, 14, 5, NULL, 'Basement sensor reports humidity spike: Possible pipe condensation', TIMESTAMP '2026-09-17 09:40:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (21, 26, 4, NULL, 'Alpine climate system heating consumption 15% above average', TIMESTAMP '2026-09-17 07:00:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (22, 36, 1, NULL, 'Server closet door opened outside maintenance window', TIMESTAMP '2026-09-17 15:30:00', 'THRESHOLD', 'ACKNOWLEDGED', 6, TIMESTAMP '2026-09-17 15:32:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (23, 42, 1, NULL, 'Vehicular approach detected on cedar driveway after midnight', TIMESTAMP '2026-09-17 01:10:00', 'THRESHOLD', 'ACKNOWLEDGED', 7, TIMESTAMP '2026-09-17 01:12:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (24, 2, 3, NULL, 'Living Room Chandelier: LED driver operating temperature elevated', TIMESTAMP '2026-09-16 20:00:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (25, 12, 4, NULL, 'Office light active for 12 continuous hours without user motion', TIMESTAMP '2026-09-16 22:00:00', 'THRESHOLD', 'ACKNOWLEDGED', 1, TIMESTAMP '2026-09-16 22:05:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (26, 32, 3, NULL, 'Balcony camera lens requires cleaning: Partial obstruction reported', TIMESTAMP '2026-09-15 09:00:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (27, 39, 4, NULL, 'Cedar residence HVAC filter replacement reminder (90 days elapsed)', TIMESTAMP '2026-09-14 08:00:00', 'THRESHOLD', 'ACKNOWLEDGED', 7, TIMESTAMP '2026-09-14 10:00:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (28, 18, 2, NULL, 'Malibu HVAC external humidity sensor reading saturated', TIMESTAMP '2026-09-17 06:30:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (29, 25, 1, NULL, 'Wine cellar access sensor triggered without lighting activity', TIMESTAMP '2026-09-17 00:45:00', 'THRESHOLD', 'ACKNOWLEDGED', 5, TIMESTAMP '2026-09-17 00:50:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (30, 45, 1, 15, 'Dining room motion sensor activated during away state', TIMESTAMP '2026-09-17 19:00:20', 'RULE', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (31, 11, 1, 4, 'Alpine Great Room occupancy verified by motion sensor', TIMESTAMP '2026-09-17 18:30:00', 'RULE', 'ACKNOWLEDGED', 3, TIMESTAMP '2026-09-17 18:35:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (32, 24, 1, 9, 'Master suite movement triggered night pathway lighting', TIMESTAMP '2026-09-17 23:10:05', 'RULE', 'ACKNOWLEDGED', 5, TIMESTAMP '2026-09-17 23:15:00');

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (33, 13, 2, NULL, 'Office temperature reached comfortable 22.4C setpoint', TIMESTAMP '2026-09-17 13:00:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (34, 35, 2, NULL, 'Chicago studio bedroom optimal sleeping climate confirmed', TIMESTAMP '2026-09-17 22:00:00', 'THRESHOLD', 'ACTIVE', NULL, NULL);

INSERT INTO ALERTS (alert_id, device_id, category_id, triggered_by_rule_id, message, alert_time, source, status, acknowledged_by, acknowledged_timestamp)
VALUES (35, 43, 3, NULL, 'Primary Suite Recessed Lights firmware update available', TIMESTAMP '2026-09-17 12:00:00', 'THRESHOLD', 'ACKNOWLEDGED', 8, TIMESTAMP '2026-09-17 12:10:00');


-- ----------------------------------------------------------------------------
-- 13. NOTIFICATION_PREFERENCES (Inserts 20 new ternary links -> Total: 25)
-- ----------------------------------------------------------------------------
INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (1, 14, 2, 'SMS', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (1, 3, 4, 'EMAIL', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (2, 6, 1, 'PUSH', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (2, 29, 4, 'EMAIL', 0);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (3, 8, 2, 'SMS', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (3, 11, 1, 'PUSH', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (4, 4, 2, 'EMAIL', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (5, 21, 1, 'PUSH', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (5, 23, 2, 'SMS', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (5, 18, 4, 'EMAIL', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (5, 21, 5, 'PUSH', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (6, 33, 2, 'SMS', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (6, 36, 1, 'PUSH', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (6, 32, 1, 'EMAIL', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (7, 41, 1, 'PUSH', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (7, 42, 1, 'SMS', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (7, 40, 2, 'EMAIL', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (7, 39, 4, 'EMAIL', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (8, 43, 3, 'PUSH', 1);

INSERT INTO NOTIFICATION_PREFERENCES (user_id, device_id, category_id, channel, enabled)
VALUES (8, 23, 2, 'SMS', 0);

COMMIT;
