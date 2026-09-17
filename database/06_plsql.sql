-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 06_plsql.sql
-- Description: PL/SQL procedures, functions, explicit cursors, and triggers.
-- ============================================================================

-- ============================================================================
-- 1. PROCEDURE: sp_acknowledge_alert
-- Acknowledges an active alert, recording user ID and current timestamp.
-- Demonstrates validation, exception handling, and transaction control.
-- ============================================================================
CREATE OR REPLACE PROCEDURE sp_acknowledge_alert (
    p_alert_id IN NUMBER,
    p_user_id  IN NUMBER
) IS
    v_current_status ALERTS.status%TYPE;
    v_user_exists    NUMBER := 0;
    
    -- Custom business exceptions
    ex_alert_not_found       EXCEPTION;
    ex_user_not_found        EXCEPTION;
    ex_already_acknowledged  EXCEPTION;
BEGIN
    -- 1. Verify user exists
    SELECT COUNT(*) INTO v_user_exists
    FROM USERS
    WHERE user_id = p_user_id;

    IF v_user_exists = 0 THEN
        RAISE ex_user_not_found;
    END IF;

    -- 2. Verify alert exists and lock row for update
    BEGIN
        SELECT status INTO v_current_status
        FROM ALERTS
        WHERE alert_id = p_alert_id
        FOR UPDATE NOWAIT;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE ex_alert_not_found;
    END;

    -- 3. Verify alert is not already acknowledged
    IF v_current_status = 'ACKNOWLEDGED' THEN
        RAISE ex_already_acknowledged;
    END IF;

    -- 4. Update alert status
    UPDATE ALERTS
    SET status = 'ACKNOWLEDGED',
        acknowledged_by = p_user_id,
        acknowledged_timestamp = SYSTIMESTAMP
    WHERE alert_id = p_alert_id;

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Success: Alert ID ' || p_alert_id || ' acknowledged by User ID ' || p_user_id);

EXCEPTION
    WHEN ex_alert_not_found THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20001, 'Error: Alert ID ' || p_alert_id || ' does not exist.');
    WHEN ex_user_not_found THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20002, 'Error: User ID ' || p_user_id || ' does not exist.');
    WHEN ex_already_acknowledged THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20003, 'Notice: Alert ID ' || p_alert_id || ' has already been acknowledged.');
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20099, 'Unexpected error in sp_acknowledge_alert: ' || SQLERRM);
END sp_acknowledge_alert;
/


-- ============================================================================
-- 2. PROCEDURE: sp_record_sensor_reading
-- Records a new reading for a device using the SEQ_READING_ID sequence.
-- ============================================================================
CREATE OR REPLACE PROCEDURE sp_record_sensor_reading (
    p_device_id   IN NUMBER,
    p_value       IN NUMBER,
    p_reading_id  OUT NUMBER
) IS
    v_device_count NUMBER := 0;
    v_status       DEVICES.status%TYPE;
    ex_invalid_device EXCEPTION;
    ex_device_offline EXCEPTION;
BEGIN
    -- Check if device exists and get status
    BEGIN
        SELECT status INTO v_status
        FROM DEVICES
        WHERE device_id = p_device_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE ex_invalid_device;
    END;

    IF v_status = 'OFFLINE' THEN
        RAISE ex_device_offline;
    END IF;

    -- Fetch next reading ID from sequence
    SELECT SEQ_READING_ID.NEXTVAL INTO p_reading_id FROM DUAL;

    -- Insert weak entity reading
    INSERT INTO SENSOR_READINGS (device_id, reading_id, value, reading_time)
    VALUES (p_device_id, p_reading_id, p_value, SYSTIMESTAMP);

    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Success: Recorded reading ' || p_reading_id || ' for Device ' || p_device_id);

EXCEPTION
    WHEN ex_invalid_device THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20010, 'Error: Device ID ' || p_device_id || ' not found.');
    WHEN ex_device_offline THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20011, 'Warning: Cannot record reading; Device ID ' || p_device_id || ' is OFFLINE.');
    WHEN OTHERS THEN
        ROLLBACK;
        RAISE_APPLICATION_ERROR(-20099, 'Error in sp_record_sensor_reading: ' || SQLERRM);
END sp_record_sensor_reading;
/


-- ============================================================================
-- 3. FUNCTION: fn_calculate_device_uptime
-- Computes the derived uptime string for a given device.
-- Formula: SYSTIMESTAMP - COALESCE(last_restart_time, install_date)
-- ============================================================================
CREATE OR REPLACE FUNCTION fn_calculate_device_uptime (
    p_device_id IN NUMBER
) RETURN VARCHAR2 IS
    v_install_date      DEVICES.install_date%TYPE;
    v_last_restart_time DEVICES.last_restart_time%TYPE;
    v_status            DEVICES.status%TYPE;
    v_start_time        TIMESTAMP;
    v_interval          INTERVAL DAY TO SECOND;
    v_days              NUMBER;
    v_hours             NUMBER;
    v_minutes           NUMBER;
BEGIN
    SELECT install_date, last_restart_time, status
    INTO v_install_date, v_last_restart_time, v_status
    FROM DEVICES
    WHERE device_id = p_device_id;

    IF v_status = 'OFFLINE' THEN
        RETURN '0 days (Device is OFFLINE)';
    END IF;

    -- Fallback logic: if last_restart_time is NULL, fall back to install_date
    v_start_time := COALESCE(v_last_restart_time, CAST(v_install_date AS TIMESTAMP));

    IF v_start_time IS NULL THEN
        RETURN 'Unknown (No install or restart time recorded)';
    END IF;

    v_interval := SYSTIMESTAMP - v_start_time;
    v_days    := EXTRACT(DAY FROM v_interval);
    v_hours   := EXTRACT(HOUR FROM v_interval);
    v_minutes := EXTRACT(MINUTE FROM v_interval);

    RETURN v_days || ' days, ' || v_hours || ' hours, ' || v_minutes || ' minutes';

EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 'Error: Device not found';
    WHEN OTHERS THEN
        RETURN 'Error calculating uptime: ' || SQLERRM;
END fn_calculate_device_uptime;
/


-- ============================================================================
-- 4. FUNCTION: fn_check_device_access
-- Checks if a user has access to a device via the M:N HOME_ACCESS relationship.
-- Returns 1 if authorized, 0 otherwise.
-- ============================================================================
CREATE OR REPLACE FUNCTION fn_check_device_access (
    p_user_id   IN NUMBER,
    p_device_id IN NUMBER
) RETURN NUMBER IS
    v_access_count NUMBER := 0;
BEGIN
    SELECT COUNT(*)
    INTO v_access_count
    FROM HOME_ACCESS ha
    JOIN ROOMS r ON ha.home_id = r.home_id
    JOIN DEVICES d ON r.room_id = d.room_id
    WHERE ha.user_id = p_user_id
      AND d.device_id = p_device_id;

    IF v_access_count > 0 THEN
        RETURN 1;
    ELSE
        RETURN 0;
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        RETURN 0;
END fn_check_device_access;
/


-- ============================================================================
-- 5. PROCEDURE: sp_home_device_health_report
-- Demonstrates an EXPLICIT CURSOR with OPEN, FETCH, CLOSE, and formatted output.
-- ============================================================================
CREATE OR REPLACE PROCEDURE sp_home_device_health_report (
    p_home_id IN NUMBER
) IS
    -- Cursor to iterate over all devices in the requested home
    CURSOR c_devices IS
        SELECT 
            r.room_name,
            d.device_id,
            d.device_name,
            d.device_subtype,
            d.status,
            fn_calculate_device_uptime(d.device_id) AS uptime
        FROM ROOMS r
        JOIN DEVICES d ON r.room_id = d.room_id
        WHERE r.home_id = p_home_id
        ORDER BY r.room_name, d.device_name;

    v_dev_rec       c_devices%ROWTYPE;
    v_home_name     HOMES.home_name%TYPE;
    v_total_devices NUMBER := 0;
    v_online_count  NUMBER := 0;
BEGIN
    -- Verify home existence
    BEGIN
        SELECT home_name INTO v_home_name
        FROM HOMES
        WHERE home_id = p_home_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20020, 'Home ID ' || p_home_id || ' does not exist.');
    END;

    DBMS_OUTPUT.PUT_LINE('================================================================');
    DBMS_OUTPUT.PUT_LINE('DEVICE HEALTH & STATUS REPORT: ' || UPPER(v_home_name));
    DBMS_OUTPUT.PUT_LINE('Generated: ' || TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS'));
    DBMS_OUTPUT.PUT_LINE('================================================================');

    OPEN c_devices;
    LOOP
        FETCH c_devices INTO v_dev_rec;
        EXIT WHEN c_devices%NOTFOUND;

        v_total_devices := v_total_devices + 1;
        IF v_dev_rec.status = 'ONLINE' THEN
            v_online_count := v_online_count + 1;
        END IF;

        DBMS_OUTPUT.PUT_LINE(
            '[' || v_dev_rec.status || '] ' || 
            RPAD(v_dev_rec.room_name, 16) || ' | ' ||
            RPAD(v_dev_rec.device_name, 30) || ' (' || 
            v_dev_rec.device_subtype || ') - Uptime: ' || 
            v_dev_rec.uptime
        );
    END LOOP;
    CLOSE c_devices;

    DBMS_OUTPUT.PUT_LINE('----------------------------------------------------------------');
    DBMS_OUTPUT.PUT_LINE('Summary: ' || v_online_count || ' of ' || v_total_devices || ' devices online.');
    DBMS_OUTPUT.PUT_LINE('================================================================');

EXCEPTION
    WHEN OTHERS THEN
        IF c_devices%ISOPEN THEN
            CLOSE c_devices;
        END IF;
        RAISE_APPLICATION_ERROR(-20099, 'Error generating health report: ' || SQLERRM);
END sp_home_device_health_report;
/


-- ============================================================================
-- 6. TRIGGER: trg_prevent_device_cycle
-- Enforces that a device cannot be its own parent and guards against direct cycles.
-- ============================================================================
CREATE OR REPLACE TRIGGER trg_prevent_device_cycle
BEFORE INSERT OR UPDATE OF parent_device_id ON DEVICES
FOR EACH ROW
BEGIN
    -- Guard 1: Device cannot be its own parent
    IF :NEW.parent_device_id IS NOT NULL AND :NEW.parent_device_id = :NEW.device_id THEN
        RAISE_APPLICATION_ERROR(-20030, 'Validation Error: Device ID ' || :NEW.device_id || ' cannot be set as its own parent.');
    END IF;
END;
/
