-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 05_queries.sql
-- Description: Core analytical and operational queries demonstrating the EER design.
-- ============================================================================

-- ============================================================================
-- 1. USERS AND HOMES THROUGH M:N HOME_ACCESS
-- Lists all users, their associated properties, granted role, and date granted.
-- ============================================================================
SELECT 
    u.user_id,
    u.name AS user_name,
    u.email,
    h.home_id,
    h.home_name,
    h.city,
    ha.role AS home_role,
    TO_CHAR(ha.date_granted, 'YYYY-MM-DD') AS access_granted_date
FROM USERS u
JOIN HOME_ACCESS ha ON u.user_id = ha.user_id
JOIN HOMES h ON ha.home_id = h.home_id
ORDER BY u.user_id, h.home_id;


-- ============================================================================
-- 2. HOMES ACCESSIBLE TO A SPECIFIC USER (e.g. user_id = 1)
-- Demonstrates user-specific authorization boundary.
-- ============================================================================
SELECT 
    h.home_id,
    h.home_name,
    h.street,
    h.city,
    h.pincode,
    ha.role,
    ha.date_granted
FROM HOMES h
JOIN HOME_ACCESS ha ON h.home_id = ha.home_id
WHERE ha.user_id = 1
ORDER BY ha.role DESC, h.home_name ASC;


-- ============================================================================
-- 3. ROOMS BELONGING TO A HOME (e.g. home_id = 1)
-- Joined with HOMES to fetch home metadata without transitive dependencies.
-- ============================================================================
SELECT 
    r.room_id,
    r.room_name,
    r.floor_number,
    h.home_name,
    h.city
FROM ROOMS r
JOIN HOMES h ON r.home_id = h.home_id
WHERE r.home_id = 1
ORDER BY r.floor_number ASC, r.room_name ASC;


-- ============================================================================
-- 4. ALL DEVICES IN HOMES ACCESSIBLE TO A USER (e.g. user_id = 1)
-- Complete security traversal: User -> HOME_ACCESS -> Home -> Room -> Device.
-- ============================================================================
SELECT 
    u.name AS requested_by,
    h.home_name,
    r.room_name,
    d.device_id,
    d.device_name,
    d.device_subtype,
    d.status
FROM USERS u
JOIN HOME_ACCESS ha ON u.user_id = ha.user_id
JOIN HOMES h ON ha.home_id = h.home_id
JOIN ROOMS r ON h.home_id = r.home_id
JOIN DEVICES d ON r.room_id = d.room_id
WHERE u.user_id = 1
ORDER BY h.home_name, r.room_name, d.device_name;


-- ============================================================================
-- 5. DEVICE SUBTYPE DETAILS (Specialization Join)
-- Shows how base DEVICES attributes merge with subtype-specific attributes.
-- ============================================================================
SELECT 
    d.device_id,
    d.device_name,
    d.device_subtype,
    d.status,
    -- Smart Light attributes
    sl.brightness,
    sl.color_support,
    -- Thermostat attributes ("MODE" is quoted because MODE is an Oracle reserved word)
    th.target_temperature,
    th."MODE" AS thermostat_mode,
    -- Temperature Sensor attributes
    ts.unit AS temp_unit,
    ts.min_range AS temp_min,
    ts.max_range AS temp_max,
    -- Motion Sensor attributes
    ms.sensitivity_level,
    ms.detection_range,
    -- Camera attributes
    c.resolution,
    c.storage_type,
    c.night_vision_support
FROM DEVICES d
LEFT JOIN SMART_LIGHTS sl ON d.device_id = sl.device_id
LEFT JOIN THERMOSTATS th ON d.device_id = th.device_id
LEFT JOIN TEMPERATURE_SENSORS ts ON d.device_id = ts.device_id
LEFT JOIN MOTION_SENSORS ms ON d.device_id = ms.device_id
LEFT JOIN CAMERAS c ON d.device_id = c.device_id
ORDER BY d.device_id;


-- ============================================================================
-- 6. HIERARCHICAL RECURSIVE CONTROLS QUERY (Unary Device -> Device)
-- Traverses parent hub and subordinate child devices using CONNECT BY.
-- ============================================================================
SELECT 
    LEVEL AS hierarchy_depth,
    LPAD(' ', 2 * (LEVEL - 1)) || d.device_name AS device_tree,
    d.device_id,
    d.parent_device_id,
    d.device_subtype,
    d.status
FROM DEVICES d
START WITH d.parent_device_id IS NULL
CONNECT BY PRIOR d.device_id = d.parent_device_id
ORDER SIBLINGS BY d.device_name;


-- ============================================================================
-- 7. DERIVED UPTIME CALCULATION (Uptime is derived, NEVER stored)
-- Formula: SYSTIMESTAMP - COALESCE(last_restart_time, install_date)
-- Outputs exact days, hours, and minutes of continuous operation.
-- ============================================================================
SELECT 
    d.device_id,
    d.device_name,
    d.status,
    d.install_date,
    d.last_restart_time,
    -- Calculate interval from restart time, falling back to install_date
    SYSTIMESTAMP - COALESCE(d.last_restart_time, CAST(d.install_date AS TIMESTAMP)) AS uptime_interval,
    -- Formatted human-readable uptime
    EXTRACT(DAY FROM (SYSTIMESTAMP - COALESCE(d.last_restart_time, CAST(d.install_date AS TIMESTAMP)))) || ' days, ' ||
    EXTRACT(HOUR FROM (SYSTIMESTAMP - COALESCE(d.last_restart_time, CAST(d.install_date AS TIMESTAMP)))) || ' hours, ' ||
    EXTRACT(MINUTE FROM (SYSTIMESTAMP - COALESCE(d.last_restart_time, CAST(d.install_date AS TIMESTAMP)))) || ' minutes' AS uptime_display
FROM DEVICES d
WHERE d.status = 'ONLINE'
ORDER BY d.device_id;


-- ============================================================================
-- 8. SENSOR READINGS TIME SERIES FOR A DEVICE (Weak Entity Query)
-- ============================================================================
SELECT 
    d.device_id,
    d.device_name,
    sr.reading_id,
    sr.value,
    ts.unit,
    TO_CHAR(sr.reading_time, 'YYYY-MM-DD HH24:MI:SS') AS timestamp_str
FROM SENSOR_READINGS sr
JOIN DEVICES d ON sr.device_id = d.device_id
LEFT JOIN TEMPERATURE_SENSORS ts ON d.device_id = ts.device_id
WHERE sr.device_id = 4
ORDER BY sr.reading_time DESC;


-- ============================================================================
-- 9. ACTIVE AND UNACKNOWLEDGED ALERTS WITH CATEGORY AND LOCATION DETAILS
-- ============================================================================
SELECT 
    a.alert_id,
    a.alert_time,
    ac.category_name,
    ac.default_severity,
    d.device_name,
    r.room_name,
    h.home_name,
    a.source,
    a.message,
    a.status
FROM ALERTS a
JOIN DEVICES d ON a.device_id = d.device_id
JOIN ROOMS r ON d.room_id = r.room_id
JOIN HOMES h ON r.home_id = h.home_id
JOIN ALERT_CATEGORIES ac ON a.category_id = ac.category_id
WHERE a.status = 'ACTIVE' AND a.acknowledged_by IS NULL
ORDER BY a.alert_time DESC;


-- ============================================================================
-- 10. AUTOMATION RULES AND THEIR TRIGGERED ACTIONS
-- ============================================================================
SELECT 
    ar.rule_id,
    ar.rule_name,
    d_cond.device_name AS condition_source_device,
    ar.condition_operator || ' ' || ar.condition_value AS condition_expression,
    u.name AS rule_creator,
    ra.action_id,
    ra.action_type,
    ra.action_value,
    d_target.device_name AS target_actuator_device
FROM AUTOMATION_RULES ar
JOIN DEVICES d_cond ON ar.condition_device_id = d_cond.device_id
JOIN USERS u ON ar.created_by_user_id = u.user_id
LEFT JOIN RULE_ACTIONS ra ON ar.rule_id = ra.rule_id
LEFT JOIN DEVICES d_target ON ra.target_device_id = d_target.device_id
ORDER BY ar.rule_id, ra.action_id;


-- ============================================================================
-- 11. NOTIFICATION PREFERENCES TERNARY AUDIT
-- Gathers ternary configuration (User x Device x AlertCategory)
-- ============================================================================
SELECT 
    u.name AS user_name,
    u.email,
    d.device_name,
    ac.category_name,
    np.channel,
    CASE np.enabled WHEN 1 THEN 'ENABLED' ELSE 'DISABLED' END AS subscription_status
FROM NOTIFICATION_PREFERENCES np
JOIN USERS u ON np.user_id = u.user_id
JOIN DEVICES d ON np.device_id = d.device_id
JOIN ALERT_CATEGORIES ac ON np.category_id = ac.category_id
ORDER BY u.name, d.device_name, ac.category_name;


-- ============================================================================
-- 12. DASHBOARD AGGREGATE QUERIES
-- Summary statistics suitable for real-time frontend dashboard cards.
-- ============================================================================

-- 12.1 Device Status Breakdown
SELECT 
    d.status, 
    COUNT(*) AS count_devices
FROM DEVICES d
GROUP BY d.status
ORDER BY count_devices DESC;

-- 12.2 Alert Summary by Category and Status
SELECT 
    ac.category_name,
    a.status,
    COUNT(*) AS total_alerts
FROM ALERTS a
JOIN ALERT_CATEGORIES ac ON a.category_id = ac.category_id
GROUP BY ac.category_name, a.status
ORDER BY ac.category_name, a.status;

-- 12.3 Device Count by Home and Room
SELECT 
    h.home_name,
    r.room_name,
    COUNT(d.device_id) AS total_devices
FROM HOMES h
JOIN ROOMS r ON h.home_id = r.home_id
LEFT JOIN DEVICES d ON r.room_id = d.room_id
GROUP BY h.home_name, r.room_name
ORDER BY h.home_name, total_devices DESC;

-- 12.4 Average, Minimum, and Maximum Sensor Readings by Device
SELECT 
    d.device_id,
    d.device_name,
    ROUND(AVG(sr.value), 2) AS avg_reading,
    MIN(sr.value) AS min_reading,
    MAX(sr.value) AS max_reading,
    COUNT(sr.reading_id) AS total_readings
FROM DEVICES d
JOIN SENSOR_READINGS sr ON d.device_id = sr.device_id
GROUP BY d.device_id, d.device_name
ORDER BY d.device_id;
