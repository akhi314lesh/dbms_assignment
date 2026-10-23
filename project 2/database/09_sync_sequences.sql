-- ============================================================================
-- Smart Home / IoT Device Monitoring System
-- Script: 09_sync_sequences.sql
-- Description: Moves every sequence past the highest ID already present.
--
-- Why: 04_sample_data.sql inserts rows with explicit IDs (homes 1-5, devices
-- 1-45, readings 1-120, ...) but 03_sequences.sql starts every sequence at 1.
-- Without this script the first "Create Home" / new device / new reading made
-- from the website fails with ORA-00001 (unique constraint violated).
-- Safe to run any number of times. Requires Oracle 18c+ (RESTART clause).
-- ============================================================================
SET SERVEROUTPUT ON;

DECLARE
    PROCEDURE sync(p_seq VARCHAR2, p_table VARCHAR2, p_col VARCHAR2) IS
        v_max NUMBER;
    BEGIN
        EXECUTE IMMEDIATE 'SELECT NVL(MAX(' || p_col || '), 0) FROM ' || p_table INTO v_max;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE ' || p_seq || ' RESTART START WITH ' || (v_max + 1);
        DBMS_OUTPUT.PUT_LINE(RPAD(p_seq, 18) || ' -> next value ' || (v_max + 1));
    END;
BEGIN
    sync('SEQ_USER_ID',     'USERS',            'user_id');
    sync('SEQ_HOME_ID',     'HOMES',            'home_id');
    sync('SEQ_ROOM_ID',     'ROOMS',            'room_id');
    sync('SEQ_DEVICE_ID',   'DEVICES',          'device_id');
    sync('SEQ_READING_ID',  'SENSOR_READINGS',  'reading_id');
    sync('SEQ_RULE_ID',     'AUTOMATION_RULES', 'rule_id');
    sync('SEQ_ACTION_ID',   'RULE_ACTIONS',     'action_id');
    sync('SEQ_CATEGORY_ID', 'ALERT_CATEGORIES', 'category_id');
    sync('SEQ_ALERT_ID',    'ALERTS',           'alert_id');
END;
/
