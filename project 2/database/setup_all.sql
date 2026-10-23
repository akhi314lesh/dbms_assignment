-- ============================================================================
-- One-shot schema setup (run as SMART_HOME, connected to XEPDB1):
--
--   cd database
--   sqlplus SMART_HOME/SmartHomeDev123@localhost:1521/XEPDB1 @setup_all.sql
--
-- Runs the scripts in the correct order. 04b_expand_sample_data.sql is NOT run:
-- 04_sample_data.sql already contains the complete dataset, and running 04b on
-- top of it only produces hundreds of ORA-00001 duplicate-key errors.
-- The benchmark tables (08) are created automatically by the backend.
-- ============================================================================
SET DEFINE OFF
SET FEEDBACK OFF
PROMPT === 01 tables ===
@@01_create_tables.sql
PROMPT === 02 constraints ===
@@02_constraints.sql
PROMPT === 03 sequences ===
@@03_sequences.sql
PROMPT === 04 sample data ===
@@04_sample_data.sql
PROMPT === 06 PL/SQL ===
@@06_plsql.sql
PROMPT === 07 auth extension ===
@@07_auth_extension.sql
PROMPT === 09 sync sequences ===
@@09_sync_sequences.sql
PROMPT === Row counts ===
@@exact_count_all_17.sql
PROMPT === Setup complete ===
EXIT
