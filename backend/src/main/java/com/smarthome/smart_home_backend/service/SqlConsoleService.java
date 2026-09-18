package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.dto.PlSqlExecuteRequest;
import com.smarthome.smart_home_backend.dto.SqlConsoleRequest;
import com.smarthome.smart_home_backend.dto.SqlConsoleResponse;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SqlConsoleService {

    private final JdbcTemplate jdbcTemplate;
    private final HomeAuthorizationService authService;
    private final TransactionTemplate transactionTemplate;

    private static final Pattern PROHIBITED_GLOBAL = Pattern.compile(
            "\\b(DROP|ALTER|TRUNCATE|CREATE|RENAME|COMMENT|GRANT|REVOKE|BEGIN|DECLARE|EXEC|EXECUTE|DBMS_\\w+|UTL_\\w+|SYS\\.\\w+|DBA_\\w+|ALL_\\w+|V\\$\\w+|USER_TABLES|USER_VIEWS|USER_OBJECTS|USER_CONSTRAINTS|USER_INDEXES|USER_SEQUENCES|USER_TRIGGERS|USER_PROCEDURES|USER_TAB_COLUMNS)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PROHIBITED_USER_IDENTITY_TABLES = Pattern.compile(
            "\\b(USERS|USER_CONTACT_NUMBERS|HOME_ACCESS)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> ALLOWED_USER_TABLES = Set.of(
            "HOMES", "ROOMS", "DEVICES", "SMART_LIGHTS", "THERMOSTATS",
            "TEMPERATURE_SENSORS", "MOTION_SENSORS", "CAMERAS", "SENSOR_READINGS",
            "ALERTS", "ALERT_CATEGORIES", "AUTOMATION_RULES", "RULE_ACTIONS",
            "NOTIFICATION_PREFERENCES", "DUAL"
    );

    public SqlConsoleService(JdbcTemplate jdbcTemplate,
                             HomeAuthorizationService authService,
                             PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public SqlConsoleResponse executeQuery(SqlConsoleRequest request) {
        long startTime = System.currentTimeMillis();

        if (request == null || request.getSql() == null || request.getSql().trim().isEmpty()) {
            return SqlConsoleResponse.error("SQL query cannot be empty.", 0);
        }

        String rawSql = request.getSql().trim();

        // Check for multiple statements (reject semicolon injection)
        String cleanedSql = rawSql;
        if (cleanedSql.endsWith(";")) {
            cleanedSql = cleanedSql.substring(0, cleanedSql.length() - 1).trim();
        }
        if (cleanedSql.contains(";")) {
            return SqlConsoleResponse.error("Security violation: Multiple SQL statements are not permitted.", System.currentTimeMillis() - startTime);
        }

        // Check global prohibitions (DDL, shell, anonymous PL/SQL blocks, system catalogs)
        Matcher prohibitedMatcher = PROHIBITED_GLOBAL.matcher(cleanedSql);
        if (prohibitedMatcher.find()) {
            return SqlConsoleResponse.error("Security violation: Operation '" + prohibitedMatcher.group(1).toUpperCase() + "' is prohibited.", System.currentTimeMillis() - startTime);
        }

        User currentUser = authService.requireCurrentUser();
        boolean isAdmin = authService.isAdmin(currentUser);

        String statementType = cleanedSql.split("\\s+")[0].toUpperCase();

        if (!isAdmin) {
            // USER permission checks

            // Reject querying or mutating user accounts or security access tables
            Matcher userTablesMatcher = PROHIBITED_USER_IDENTITY_TABLES.matcher(cleanedSql);
            if (userTablesMatcher.find()) {
                return SqlConsoleResponse.error("Access denied: Accessing user identity or authorization tables is restricted to administrators.", System.currentTimeMillis() - startTime);
            }

            List<Long> accessibleHomeIds = authService.getAccessibleHomeIds(currentUser);

            if ("SELECT".equalsIgnoreCase(statementType)) {
                String scopedSql = scopeUserSelect(cleanedSql, accessibleHomeIds, currentUser.getUserId());
                return executeSelect(scopedSql, startTime);
            } else if ("INSERT".equalsIgnoreCase(statementType)) {
                return executeUserInsert(cleanedSql, accessibleHomeIds, currentUser, startTime);
            } else if ("UPDATE".equalsIgnoreCase(statementType)) {
                return executeUserUpdate(cleanedSql, accessibleHomeIds, currentUser, startTime);
            } else if ("DELETE".equalsIgnoreCase(statementType)) {
                return executeUserDelete(cleanedSql, accessibleHomeIds, currentUser, startTime);
            } else {
                return SqlConsoleResponse.error("Unsupported statement type '" + statementType + "'. Allowed: SELECT, INSERT, UPDATE, DELETE.", System.currentTimeMillis() - startTime);
            }
        } else {
            // ADMIN execution
            if ("SELECT".equalsIgnoreCase(statementType)) {
                return executeSelect(cleanedSql, startTime);
            } else if ("INSERT".equalsIgnoreCase(statementType) || "UPDATE".equalsIgnoreCase(statementType) || "DELETE".equalsIgnoreCase(statementType)) {
                return executeDml(cleanedSql, startTime);
            } else {
                return SqlConsoleResponse.error("Unsupported statement type '" + statementType + "'. Allowed: SELECT, INSERT, UPDATE, DELETE.", System.currentTimeMillis() - startTime);
            }
        }
    }

    public SqlConsoleResponse executePlSql(PlSqlExecuteRequest request) {
        long startTime = System.currentTimeMillis();
        if (request == null || request.getRoutineName() == null || request.getRoutineName().trim().isEmpty()) {
            return SqlConsoleResponse.error("Routine name cannot be empty.", 0);
        }

        String routine = request.getRoutineName().trim().toUpperCase();
        Map<String, Object> params = request.getParams() != null ? request.getParams() : Collections.emptyMap();

        User currentUser = authService.requireCurrentUser();
        boolean isAdmin = authService.isAdmin(currentUser);
        List<Long> accessibleHomeIds = authService.getAccessibleHomeIds(currentUser);

        try {
            switch (routine) {
                case "FN_CALCULATE_DEVICE_UPTIME": {
                    Long deviceId = getLongParam(params, "deviceId", "device_id");
                    if (deviceId == null) {
                        return SqlConsoleResponse.error("Missing required parameter 'deviceId'.", System.currentTimeMillis() - startTime);
                    }
                    if (!isAdmin && !isDeviceAccessible(deviceId, accessibleHomeIds)) {
                        return SqlConsoleResponse.error("Access denied: Device ID " + deviceId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
                    }
                    String uptime = jdbcTemplate.queryForObject("SELECT fn_calculate_device_uptime(?) FROM DUAL", String.class, deviceId);
                    long elapsed = System.currentTimeMillis() - startTime;
                    List<String> cols = List.of("DEVICE_ID", "UPTIME");
                    List<Map<String, Object>> rows = List.of(Map.of("DEVICE_ID", deviceId, "UPTIME", uptime != null ? uptime : "N/A"));
                    return SqlConsoleResponse.querySuccess(cols, rows, elapsed, "SELECT fn_calculate_device_uptime(" + deviceId + ") FROM DUAL");
                }

                case "FN_CHECK_DEVICE_ACCESS": {
                    Long deviceId = getLongParam(params, "deviceId", "device_id");
                    if (deviceId == null) {
                        return SqlConsoleResponse.error("Missing required parameter 'deviceId'.", System.currentTimeMillis() - startTime);
                    }
                    Long targetUserId = isAdmin && params.containsKey("userId") ? getLongParam(params, "userId", "user_id") : currentUser.getUserId();
                    Integer hasAccess = jdbcTemplate.queryForObject("SELECT fn_check_device_access(?, ?) FROM DUAL", Integer.class, targetUserId, deviceId);
                    long elapsed = System.currentTimeMillis() - startTime;
                    List<String> cols = List.of("USER_ID", "DEVICE_ID", "HAS_ACCESS");
                    List<Map<String, Object>> rows = List.of(Map.of("USER_ID", targetUserId, "DEVICE_ID", deviceId, "HAS_ACCESS", hasAccess != null && hasAccess == 1 ? "1 (TRUE)" : "0 (FALSE)"));
                    return SqlConsoleResponse.querySuccess(cols, rows, elapsed, "SELECT fn_check_device_access(" + targetUserId + ", " + deviceId + ") FROM DUAL");
                }

                case "SP_ACKNOWLEDGE_ALERT": {
                    Long alertId = getLongParam(params, "alertId", "alert_id");
                    if (alertId == null) {
                        return SqlConsoleResponse.error("Missing required parameter 'alertId'.", System.currentTimeMillis() - startTime);
                    }
                    if (!isAdmin && !isAlertAccessible(alertId, accessibleHomeIds)) {
                        return SqlConsoleResponse.error("Access denied: Alert ID " + alertId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
                    }
                    Long actingUserId = currentUser.getUserId();
                    return transactionTemplate.execute(status -> {
                        jdbcTemplate.update("CALL sp_acknowledge_alert(?, ?)", alertId, actingUserId);
                        long elapsed = System.currentTimeMillis() - startTime;
                        return SqlConsoleResponse.dmlSuccess(1, elapsed, "CALL sp_acknowledge_alert(" + alertId + ", " + actingUserId + ")");
                    });
                }

                case "SP_RECORD_SENSOR_READING": {
                    Long deviceId = getLongParam(params, "deviceId", "device_id");
                    Double value = getDoubleParam(params, "value");
                    if (deviceId == null || value == null) {
                        return SqlConsoleResponse.error("Missing required parameters: 'deviceId' and 'value'.", System.currentTimeMillis() - startTime);
                    }
                    if (!isAdmin && !isDeviceAccessible(deviceId, accessibleHomeIds)) {
                        return SqlConsoleResponse.error("Access denied: Device ID " + deviceId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
                    }
                    Long generatedReadingId = jdbcTemplate.execute((Connection conn) -> {
                        try (CallableStatement cs = conn.prepareCall("CALL sp_record_sensor_reading(?, ?, ?)")) {
                            cs.setLong(1, deviceId);
                            cs.setDouble(2, value);
                            cs.registerOutParameter(3, Types.NUMERIC);
                            cs.execute();
                            return cs.getLong(3);
                        }
                    });
                    long elapsed = System.currentTimeMillis() - startTime;
                    SqlConsoleResponse resp = SqlConsoleResponse.dmlSuccess(1, elapsed, "CALL sp_record_sensor_reading(" + deviceId + ", " + value + ", ?)");
                    resp.setMessage("Success: Recorded sensor reading " + generatedReadingId + " for Device ID " + deviceId);
                    return resp;
                }

                case "SP_HOME_DEVICE_HEALTH_REPORT": {
                    Long homeId = getLongParam(params, "homeId", "home_id");
                    if (homeId == null) {
                        return SqlConsoleResponse.error("Missing required parameter 'homeId'.", System.currentTimeMillis() - startTime);
                    }
                    if (!isAdmin && (accessibleHomeIds == null || !accessibleHomeIds.contains(homeId))) {
                        return SqlConsoleResponse.error("Access denied: Home ID " + homeId + " is not accessible.", System.currentTimeMillis() - startTime);
                    }
                    String report = jdbcTemplate.execute((Connection conn) -> {
                        CallableStatement enable = null;
                        CallableStatement call = null;
                        CallableStatement getLine = null;
                        CallableStatement disable = null;
                        try {
                            enable = conn.prepareCall("BEGIN DBMS_OUTPUT.ENABLE(1000000); END;");
                            enable.execute();

                            call = conn.prepareCall("BEGIN sp_home_device_health_report(?); END;");
                            call.setLong(1, homeId);
                            call.execute();

                            getLine = conn.prepareCall("BEGIN DBMS_OUTPUT.GET_LINE(?, ?); END;");
                            getLine.registerOutParameter(1, Types.VARCHAR);
                            getLine.registerOutParameter(2, Types.NUMERIC);

                            StringBuilder sb = new StringBuilder();
                            while (true) {
                                getLine.execute();
                                int status = getLine.getInt(2);
                                if (status != 0) break;
                                sb.append(getLine.getString(1)).append("\n");
                            }

                            disable = conn.prepareCall("BEGIN DBMS_OUTPUT.DISABLE(); END;");
                            disable.execute();

                            return sb.toString();
                        } finally {
                            if (enable != null) enable.close();
                            if (call != null) call.close();
                            if (getLine != null) getLine.close();
                            if (disable != null) disable.close();
                        }
                    });
                    long elapsed = System.currentTimeMillis() - startTime;
                    List<String> cols = List.of("LINE_OUTPUT");
                    List<Map<String, Object>> rows = Arrays.stream((report != null ? report : "").split("\n"))
                            .map(line -> Map.<String, Object>of("LINE_OUTPUT", line))
                            .collect(Collectors.toList());
                    SqlConsoleResponse resp = SqlConsoleResponse.querySuccess(cols, rows, elapsed, "CALL sp_home_device_health_report(" + homeId + ")");
                    resp.setMessage("Health report generated successfully for Home ID " + homeId);
                    return resp;
                }

                case "TEST_TRIGGER_CYCLE": {
                    Long targetDeviceId = getLongParam(params, "deviceId", "device_id");
                    if (targetDeviceId == null) {
                        targetDeviceId = 1L; // default test device ID
                    }
                    if (!isAdmin && !isDeviceAccessible(targetDeviceId, accessibleHomeIds)) {
                        return SqlConsoleResponse.error("Access denied: Device ID " + targetDeviceId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
                    }
                    final Long devId = targetDeviceId;
                    try {
                        transactionTemplate.execute(status -> {
                            // Intentionally set parent_device_id to self (direct cycle)
                            jdbcTemplate.update("UPDATE devices SET parent_device_id = ? WHERE device_id = ?", devId, devId);
                            return null;
                        });
                        return SqlConsoleResponse.error("Trigger test failed: Expected TRG_PREVENT_DEVICE_CYCLE violation, but update succeeded.", System.currentTimeMillis() - startTime);
                    } catch (DataAccessException ex) {
                        String msg = ex.getMessage();
                        long elapsed = System.currentTimeMillis() - startTime;
                        if (msg != null && (msg.contains("ORA-20030") || msg.contains("cannot be set as its own parent"))) {
                            SqlConsoleResponse resp = SqlConsoleResponse.dmlSuccess(0, elapsed, "UPDATE devices SET parent_device_id = " + devId + " WHERE device_id = " + devId);
                            resp.setMessage("Trigger Verified: TRG_PREVENT_DEVICE_CYCLE rejected cycle assignment (ORA-20030). Database state preserved.");
                            return resp;
                        } else {
                            return SqlConsoleResponse.error("Trigger test error: " + sanitizeErrorMessage(msg), elapsed);
                        }
                    }
                }

                default:
                    return SqlConsoleResponse.error("Access denied: Stored routine '" + routine + "' is not on the approved routine allowlist.", System.currentTimeMillis() - startTime);
            }
        } catch (DataAccessException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String msg = sanitizeErrorMessage(e.getMessage());
            return SqlConsoleResponse.error("PL/SQL execution failed: " + msg, elapsed);
        }
    }

    private SqlConsoleResponse executeSelect(String sql, long startTime) {
        try {
            return jdbcTemplate.query(sql, rs -> {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columns = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }

                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        Object val = rs.getObject(i);
                        if (val instanceof java.sql.Timestamp ts) {
                            val = ts.toLocalDateTime().toString();
                        } else if (val instanceof java.sql.Date d) {
                            val = d.toLocalDate().toString();
                        }
                        row.put(columns.get(i - 1), val);
                    }
                    rows.add(row);
                }

                long elapsed = System.currentTimeMillis() - startTime;
                return SqlConsoleResponse.querySuccess(columns, rows, elapsed, sql);
            });
        } catch (DataAccessException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String errorMsg = sanitizeErrorMessage(e.getMessage());
            return SqlConsoleResponse.error("Query execution failed: " + errorMsg, elapsed);
        }
    }

    private SqlConsoleResponse executeDml(String sql, long startTime) {
        try {
            Integer affected = transactionTemplate.execute(status -> jdbcTemplate.update(sql));
            long elapsed = System.currentTimeMillis() - startTime;
            int count = affected != null ? affected : 0;
            return SqlConsoleResponse.dmlSuccess(count, elapsed, sql);
        } catch (DataAccessException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String errorMsg = sanitizeErrorMessage(e.getMessage());
            return SqlConsoleResponse.error("DML execution failed: " + errorMsg, elapsed);
        }
    }

    private SqlConsoleResponse executeUserInsert(String sql, List<Long> accessibleHomeIds, User currentUser, long startTime) {
        String targetTable = extractTargetTable(sql, "INSERT\\s+INTO");
        if (targetTable == null) {
            return SqlConsoleResponse.error("Invalid INSERT statement format.", System.currentTimeMillis() - startTime);
        }

        if ("HOMES".equalsIgnoreCase(targetTable)) {
            return SqlConsoleResponse.error("Permission denied: Direct SQL INSERT into HOMES is prohibited. Please use the Create Home application flow.", System.currentTimeMillis() - startTime);
        }

        if (accessibleHomeIds == null || accessibleHomeIds.isEmpty()) {
            return SqlConsoleResponse.error("Access denied: You do not have access to any homes.", System.currentTimeMillis() - startTime);
        }

        // Server-side relationship validation
        if ("ROOMS".equalsIgnoreCase(targetTable)) {
            Long homeId = extractNumericValue(sql, "home_id");
            if (homeId != null && !accessibleHomeIds.contains(homeId)) {
                return SqlConsoleResponse.error("Access denied: Target Home ID " + homeId + " is not accessible to you.", System.currentTimeMillis() - startTime);
            }
        } else if ("DEVICES".equalsIgnoreCase(targetTable)) {
            Long roomId = extractNumericValue(sql, "room_id");
            if (roomId != null && !isRoomAccessible(roomId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Room ID " + roomId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if (Set.of("SMART_LIGHTS", "THERMOSTATS", "TEMPERATURE_SENSORS", "MOTION_SENSORS", "CAMERAS", "SENSOR_READINGS", "ALERTS").contains(targetTable)) {
            Long deviceId = extractNumericValue(sql, "device_id");
            if (deviceId != null && !isDeviceAccessible(deviceId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Device ID " + deviceId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if ("AUTOMATION_RULES".equalsIgnoreCase(targetTable)) {
            Long deviceId = extractNumericValue(sql, "condition_device_id");
            if (deviceId != null && !isDeviceAccessible(deviceId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Condition Device ID " + deviceId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if ("RULE_ACTIONS".equalsIgnoreCase(targetTable)) {
            Long targetDevId = extractNumericValue(sql, "target_device_id");
            if (targetDevId != null && !isDeviceAccessible(targetDevId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Device ID " + targetDevId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if ("NOTIFICATION_PREFERENCES".equalsIgnoreCase(targetTable)) {
            Long devId = extractNumericValue(sql, "device_id");
            if (devId != null && !isDeviceAccessible(devId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Device ID " + devId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
            Long userId = extractNumericValue(sql, "user_id");
            if (userId != null && !userId.equals(currentUser.getUserId())) {
                return SqlConsoleResponse.error("Access denied: Cannot configure notification preferences for another user.", System.currentTimeMillis() - startTime);
            }
        }

        return executeDml(sql, startTime);
    }

    private SqlConsoleResponse executeUserUpdate(String sql, List<Long> accessibleHomeIds, User currentUser, long startTime) {
        String targetTable = extractTargetTable(sql, "UPDATE");
        if (targetTable == null) {
            return SqlConsoleResponse.error("Invalid UPDATE statement format.", System.currentTimeMillis() - startTime);
        }

        if (!sql.toUpperCase().contains(" WHERE ")) {
            return SqlConsoleResponse.error("Safety restriction: UPDATE statement must contain an explicit WHERE clause.", System.currentTimeMillis() - startTime);
        }

        if (accessibleHomeIds == null || accessibleHomeIds.isEmpty()) {
            return SqlConsoleResponse.error("Access denied: You do not have access to any homes.", System.currentTimeMillis() - startTime);
        }

        // Validate explicit target identifiers against user's accessible homes
        if ("DEVICES".equalsIgnoreCase(targetTable) || Set.of("SMART_LIGHTS", "THERMOSTATS", "TEMPERATURE_SENSORS", "MOTION_SENSORS", "CAMERAS").contains(targetTable)) {
            Long devId = extractNumericWhereValue(sql, "device_id");
            if (devId != null && !isDeviceAccessible(devId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Device ID " + devId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if ("ROOMS".equalsIgnoreCase(targetTable)) {
            Long roomId = extractNumericWhereValue(sql, "room_id");
            if (roomId != null && !isRoomAccessible(roomId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Room ID " + roomId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if ("ALERTS".equalsIgnoreCase(targetTable)) {
            Long alertId = extractNumericWhereValue(sql, "alert_id");
            if (alertId != null && !isAlertAccessible(alertId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Alert ID " + alertId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if ("HOMES".equalsIgnoreCase(targetTable)) {
            Long homeId = extractNumericWhereValue(sql, "home_id");
            if (homeId != null && !accessibleHomeIds.contains(homeId)) {
                return SqlConsoleResponse.error("Access denied: Home ID " + homeId + " is not accessible to you.", System.currentTimeMillis() - startTime);
            }
        }

        // Inject security home-scope predicate into WHERE clause
        String scopedSql = injectUpdateDeleteScope(sql, targetTable, accessibleHomeIds, currentUser.getUserId());
        return executeDml(scopedSql, startTime);
    }

    private SqlConsoleResponse executeUserDelete(String sql, List<Long> accessibleHomeIds, User currentUser, long startTime) {
        String targetTable = extractTargetTable(sql, "DELETE\\s+FROM");
        if (targetTable == null) {
            return SqlConsoleResponse.error("Invalid DELETE statement format.", System.currentTimeMillis() - startTime);
        }

        if ("HOMES".equalsIgnoreCase(targetTable)) {
            return SqlConsoleResponse.error("Permission denied: Direct SQL DELETE from HOMES is restricted. Please use the Delete Home management option.", System.currentTimeMillis() - startTime);
        }

        if (!sql.toUpperCase().contains(" WHERE ")) {
            return SqlConsoleResponse.error("Safety restriction: DELETE statement must contain an explicit WHERE clause.", System.currentTimeMillis() - startTime);
        }

        if (accessibleHomeIds == null || accessibleHomeIds.isEmpty()) {
            return SqlConsoleResponse.error("Access denied: You do not have access to any homes.", System.currentTimeMillis() - startTime);
        }

        // Validate explicit target identifiers against user's accessible homes
        if ("ALERTS".equalsIgnoreCase(targetTable)) {
            Long alertId = extractNumericWhereValue(sql, "alert_id");
            if (alertId != null && !isAlertAccessible(alertId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Alert ID " + alertId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if ("DEVICES".equalsIgnoreCase(targetTable) || Set.of("SMART_LIGHTS", "THERMOSTATS", "TEMPERATURE_SENSORS", "MOTION_SENSORS", "CAMERAS").contains(targetTable)) {
            Long devId = extractNumericWhereValue(sql, "device_id");
            if (devId != null && !isDeviceAccessible(devId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Device ID " + devId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        } else if ("ROOMS".equalsIgnoreCase(targetTable)) {
            Long roomId = extractNumericWhereValue(sql, "room_id");
            if (roomId != null && !isRoomAccessible(roomId, accessibleHomeIds)) {
                return SqlConsoleResponse.error("Access denied: Target Room ID " + roomId + " does not belong to an accessible home.", System.currentTimeMillis() - startTime);
            }
        }

        String scopedSql = injectUpdateDeleteScope(sql, targetTable, accessibleHomeIds, currentUser.getUserId());
        return executeDml(scopedSql, startTime);
    }

    private String injectUpdateDeleteScope(String sql, String tableName, List<Long> accessibleHomeIds, Long userId) {
        String homeIdsStr = accessibleHomeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String scopePredicate = switch (tableName.toUpperCase()) {
            case "HOMES" -> "home_id IN (" + homeIdsStr + ")";
            case "ROOMS" -> "home_id IN (" + homeIdsStr + ")";
            case "DEVICES" -> "room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))";
            case "SMART_LIGHTS", "THERMOSTATS", "TEMPERATURE_SENSORS", "MOTION_SENSORS", "CAMERAS", "SENSOR_READINGS", "ALERTS" ->
                    "device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + ")))";
            case "AUTOMATION_RULES" ->
                    "condition_device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + ")))";
            case "RULE_ACTIONS" ->
                    "rule_id IN (SELECT rule_id FROM AUTOMATION_RULES WHERE condition_device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))";
            case "NOTIFICATION_PREFERENCES" -> "user_id = " + userId;
            default -> "1=1";
        };

        Pattern wherePattern = Pattern.compile("(?i)\\bWHERE\\b");
        Matcher m = wherePattern.matcher(sql);
        if (m.find()) {
            int whereEnd = m.end();
            return sql.substring(0, whereEnd) + " (" + scopePredicate + ") AND (" + sql.substring(whereEnd) + ")";
        } else {
            return sql + " WHERE " + scopePredicate;
        }
    }

    private boolean isDeviceAccessible(Long deviceId, List<Long> accessibleHomeIds) {
        if (deviceId == null || accessibleHomeIds == null || accessibleHomeIds.isEmpty()) return false;
        String homeIdsStr = accessibleHomeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DEVICES d JOIN ROOMS r ON d.room_id = r.room_id WHERE d.device_id = ? AND r.home_id IN (" + homeIdsStr + ")",
                Integer.class, deviceId);
        return count != null && count > 0;
    }

    private boolean isRoomAccessible(Long roomId, List<Long> accessibleHomeIds) {
        if (roomId == null || accessibleHomeIds == null || accessibleHomeIds.isEmpty()) return false;
        String homeIdsStr = accessibleHomeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ROOMS WHERE room_id = ? AND home_id IN (" + homeIdsStr + ")",
                Integer.class, roomId);
        return count != null && count > 0;
    }

    private boolean isAlertAccessible(Long alertId, List<Long> accessibleHomeIds) {
        if (alertId == null || accessibleHomeIds == null || accessibleHomeIds.isEmpty()) return false;
        String homeIdsStr = accessibleHomeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ALERTS a JOIN DEVICES d ON a.device_id = d.device_id JOIN ROOMS r ON d.room_id = r.room_id WHERE a.alert_id = ? AND r.home_id IN (" + homeIdsStr + ")",
                Integer.class, alertId);
        return count != null && count > 0;
    }

    private String extractTargetTable(String sql, String prefixRegex) {
        Pattern pattern = Pattern.compile("(?i)^" + prefixRegex + "\\s+([A-Za-z0-9_]+)");
        Matcher matcher = pattern.matcher(sql.trim());
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }
        return null;
    }

    private Long extractNumericValue(String sql, String colName) {
        Pattern colPattern = Pattern.compile("(?i)INSERT\\s+INTO\\s+[A-Za-z0-9_]+\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)", Pattern.DOTALL);
        Matcher matcher = colPattern.matcher(sql);
        if (matcher.find()) {
            String[] cols = matcher.group(1).split(",");
            String[] vals = matcher.group(2).split(",");
            for (int i = 0; i < cols.length && i < vals.length; i++) {
                if (cols[i].trim().equalsIgnoreCase(colName)) {
                    String val = vals[i].trim().replaceAll("['\"]", "");
                    if (val.matches("^-?\\d+(\\.\\d+)?$")) {
                        return (long) Double.parseDouble(val);
                    }
                }
            }
        }
        return null;
    }

    private Long extractNumericWhereValue(String sql, String colName) {
        Pattern pattern = Pattern.compile("(?i)\\b" + colName + "\\b\\s*=\\s*(\\d+)");
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private Long getLongParam(Map<String, Object> params, String... keys) {
        for (String k : keys) {
            Object v = params.get(k);
            if (v != null) {
                if (v instanceof Number n) return n.longValue();
                try {
                    return Long.parseLong(String.valueOf(v).trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private Double getDoubleParam(Map<String, Object> params, String... keys) {
        for (String k : keys) {
            Object v = params.get(k);
            if (v != null) {
                if (v instanceof Number n) return n.doubleValue();
                try {
                    return Double.parseDouble(String.valueOf(v).trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private String scopeUserSelect(String sql, List<Long> accessibleHomeIds, Long userId) {
        if (accessibleHomeIds == null || accessibleHomeIds.isEmpty()) {
            return "SELECT * FROM (" + sql + ") WHERE 1=0";
        }

        String homeIdsStr = accessibleHomeIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String scoped = sql;

        scoped = replaceTable(scoped, "HOMES",
                "(SELECT * FROM HOMES WHERE home_id IN (" + homeIdsStr + "))");
        scoped = replaceTable(scoped, "ROOMS",
                "(SELECT * FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))");
        scoped = replaceTable(scoped, "DEVICES",
                "(SELECT * FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + ")))");
        scoped = replaceTable(scoped, "SMART_LIGHTS",
                "(SELECT * FROM SMART_LIGHTS WHERE device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))");
        scoped = replaceTable(scoped, "THERMOSTATS",
                "(SELECT * FROM THERMOSTATS WHERE device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))");
        scoped = replaceTable(scoped, "TEMPERATURE_SENSORS",
                "(SELECT * FROM TEMPERATURE_SENSORS WHERE device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))");
        scoped = replaceTable(scoped, "MOTION_SENSORS",
                "(SELECT * FROM MOTION_SENSORS WHERE device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))");
        scoped = replaceTable(scoped, "CAMERAS",
                "(SELECT * FROM CAMERAS WHERE device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))");
        scoped = replaceTable(scoped, "SENSOR_READINGS",
                "(SELECT * FROM SENSOR_READINGS WHERE device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))");
        scoped = replaceTable(scoped, "ALERTS",
                "(SELECT * FROM ALERTS WHERE device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))");
        scoped = replaceTable(scoped, "AUTOMATION_RULES",
                "(SELECT * FROM AUTOMATION_RULES WHERE condition_device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + "))))");
        scoped = replaceTable(scoped, "RULE_ACTIONS",
                "(SELECT * FROM RULE_ACTIONS WHERE rule_id IN (SELECT rule_id FROM AUTOMATION_RULES WHERE condition_device_id IN (SELECT device_id FROM DEVICES WHERE room_id IN (SELECT room_id FROM ROOMS WHERE home_id IN (" + homeIdsStr + ")))))");
        scoped = replaceTable(scoped, "NOTIFICATION_PREFERENCES",
                "(SELECT * FROM NOTIFICATION_PREFERENCES WHERE user_id = " + userId + ")");

        return scoped;
    }

    private String replaceTable(String sql, String tableName, String replacement) {
        Pattern pattern = Pattern.compile("(?i)(\\bFROM\\s+|\\bJOIN\\s+|\\s*,\\s*)\\b" + tableName + "\\b");
        Matcher matcher = pattern.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1) + replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String sanitizeErrorMessage(String raw) {
        if (raw == null) return "Unknown database error.";
        return raw.replaceAll("(?i)jdbc:oracle:[^;]+", "[REDACTED_CONN_STR]")
                  .replaceAll("(?i)password=[^;]+", "password=[REDACTED]");
    }
}
