-- ============================================================================
-- Smart Home / IoT Device Monitoring System — DA3 Oracle vs InfluxDB comparison
-- Database: Oracle Database 21c Express Edition (PDB: XEPDB1, Schema: SMART_HOME)
-- Script: 08_benchmark_tables.sql
--
-- OPTIONAL: the Spring Boot backend creates these two tables automatically the
-- first time you click "Generate benchmark dataset" on the comparison dashboard
-- (http://localhost:5175). Run this script only if you prefer to create them by hand.
--
-- SENSOR_BENCHMARK        : the telemetry dataset used for read benchmarks.
--                           The backend writes the SAME readings into InfluxDB
--                           (bucket sensor_telemetry, measurement sensor_benchmark).
-- SENSOR_BENCHMARK_WRITE  : scratch table used by the bulk-ingestion benchmark
--                           (emptied after every run).
-- ============================================================================

CREATE TABLE SENSOR_BENCHMARK (
    device_id     NUMBER        NOT NULL,
    reading_time  TIMESTAMP     NOT NULL,
    value         NUMBER(10,2)  NOT NULL,
    CONSTRAINT PK_SENSOR_BENCH     PRIMARY KEY (device_id, reading_time),
    CONSTRAINT FK_SENSOR_BENCH_DEV FOREIGN KEY (device_id) REFERENCES DEVICES(device_id)
);
CREATE INDEX IDX_SENSOR_BENCH_TIME ON SENSOR_BENCHMARK (reading_time);

CREATE TABLE SENSOR_BENCHMARK_WRITE (
    device_id     NUMBER        NOT NULL,
    reading_time  TIMESTAMP     NOT NULL,
    value         NUMBER(10,2)  NOT NULL,
    CONSTRAINT PK_SENSOR_BENCH_W     PRIMARY KEY (device_id, reading_time),
    CONSTRAINT FK_SENSOR_BENCH_W_DEV FOREIGN KEY (device_id) REFERENCES DEVICES(device_id)
);
CREATE INDEX IDX_SENSOR_BENCH_W_TIME ON SENSOR_BENCHMARK_WRITE (reading_time);

-- To reset:  DROP TABLE SENSOR_BENCHMARK_WRITE PURGE;  DROP TABLE SENSOR_BENCHMARK PURGE;
