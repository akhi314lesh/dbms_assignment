package com.smarthome.smart_home_backend.security;

import com.smarthome.smart_home_backend.dto.PlSqlExecuteRequest;
import com.smarthome.smart_home_backend.dto.SqlConsoleRequest;
import com.smarthome.smart_home_backend.dto.SqlConsoleResponse;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.UserRepository;
import com.smarthome.smart_home_backend.service.SqlConsoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase9SqlConsoleTest {

    @Autowired
    private SqlConsoleService sqlConsoleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.findByEmail("akhilesh07vaidya@gmail.com")
                .orElseGet(() -> userRepository.findById(11L).orElseThrow());

        // Sophia Chen has access only to Home 1
        normalUser = userRepository.findByEmail("sophia.chen@smarthome.io")
                .orElseGet(() -> userRepository.findById(4L).orElseThrow());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        FirebaseUserDetails userDetails = new FirebaseUserDetails(
                user.getFirebaseUid() != null ? user.getFirebaseUid() : "test-uid",
                user.getEmail(),
                user
        );
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testAdminCanExecuteSelect() {
        authenticateAs(adminUser);
        SqlConsoleResponse response = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("SELECT device_name, status FROM devices", 1L)
        );

        assertTrue(response.isSuccess(), "Admin SELECT should succeed");
        assertNotNull(response.getRows());
        assertTrue(response.getRowCount() > 0, "Should return devices");
        assertTrue(response.getColumns().contains("DEVICE_NAME"));
        assertTrue(response.getColumns().contains("STATUS"));
    }

    @Test
    void testAdminDdlIsProhibited() {
        authenticateAs(adminUser);
        SqlConsoleResponse response = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("DROP TABLE devices", 1L)
        );

        assertFalse(response.isSuccess(), "DDL must be prohibited even for ADMIN");
        assertTrue(response.getMessage().toLowerCase().contains("prohibited"));
    }

    @Test
    void testMultipleStatementsRejected() {
        authenticateAs(adminUser);
        SqlConsoleResponse response = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("SELECT * FROM devices; DELETE FROM devices", 1L)
        );

        assertFalse(response.isSuccess(), "Multiple statements must be rejected");
        assertTrue(response.getMessage().toLowerCase().contains("multiple sql statements"));
    }

    @Test
    void testUserSelectIsScopedToAccessibleHome() {
        authenticateAs(normalUser); // has access ONLY to Home 1

        // 1. Query for devices
        SqlConsoleResponse response = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("SELECT device_name, status FROM devices", 1L)
        );

        assertTrue(response.isSuccess(), "USER SELECT should succeed");
        assertNotNull(response.getRows());
        assertTrue(response.getRowCount() > 0 && response.getRowCount() <= 12,
                "USER should only see devices from accessible home (count: " + response.getRowCount() + ")");

        // 2. Attempt to query for Home 2 explicitly
        SqlConsoleResponse home2Response = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("SELECT home_name FROM homes WHERE home_id = 2", 1L)
        );

        assertTrue(home2Response.isSuccess());
        assertEquals(0, home2Response.getRowCount(), "Inaccessible home data must yield 0 rows for USER");
    }

    @Test
    void testUserCannotQueryIdentityTables() {
        authenticateAs(normalUser);
        SqlConsoleResponse response = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("SELECT name, email FROM users", 1L)
        );

        assertFalse(response.isSuccess(), "USER cannot query sensitive users table");
        assertTrue(response.getMessage().toLowerCase().contains("access denied"));
    }

    @Test
    void testUserCannotMutateIdentityTables() {
        authenticateAs(normalUser);
        SqlConsoleResponse response = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("UPDATE users SET name = 'Hacked' WHERE user_id = 4", 1L)
        );

        assertFalse(response.isSuccess(), "USER cannot update users table");
        assertTrue(response.getMessage().toLowerCase().contains("access denied"));
    }

    @Test
    void testUserDirectInsertIntoHomesIsProhibited() {
        authenticateAs(normalUser);
        SqlConsoleResponse response = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("INSERT INTO homes (home_id, home_name) VALUES (seq_home_id.NEXTVAL, 'Orphan Mansion')", 1L)
        );

        assertFalse(response.isSuccess(), "Direct INSERT into HOMES is prohibited for regular users");
        assertTrue(response.getMessage().toLowerCase().contains("permission denied"));
    }

    @Test
    void testUserAccessibleInsertAndUpdateAndRollback() {
        authenticateAs(normalUser);

        // 1. INSERT room into Home 1 (accessible to normalUser)
        SqlConsoleResponse insertResp = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("INSERT INTO rooms (room_id, room_name, floor_number, home_id) VALUES (seq_room_id.NEXTVAL, 'Test Suite', 1, 1)", 1L)
        );
        assertTrue(insertResp.isSuccess(), "User should be able to insert room in accessible home: " + insertResp.getMessage());
        assertEquals(1, insertResp.getAffectedRows());

        // Find the created room_id
        Long createdRoomId = jdbcTemplate.queryForObject(
                "SELECT room_id FROM rooms WHERE room_name = 'Test Suite' AND home_id = 1 FETCH FIRST 1 ROWS ONLY",
                Long.class
        );
        assertNotNull(createdRoomId);

        // 2. UPDATE room name
        SqlConsoleResponse updateResp = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("UPDATE rooms SET room_name = 'Renamed Suite' WHERE room_id = " + createdRoomId, 1L)
        );
        assertTrue(updateResp.isSuccess());
        assertEquals(1, updateResp.getAffectedRows());

        // 3. DELETE room (cleanup)
        SqlConsoleResponse deleteResp = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("DELETE FROM rooms WHERE room_id = " + createdRoomId, 1L)
        );
        assertTrue(deleteResp.isSuccess());
        assertEquals(1, deleteResp.getAffectedRows());
    }

    @Test
    void testUserForeignInsertAndUpdateAreRejected() {
        authenticateAs(normalUser); // Only has access to Home 1

        // 1. Attempt INSERT room into Home 2 (foreign)
        SqlConsoleResponse insertResp = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("INSERT INTO rooms (room_id, room_name, floor_number, home_id) VALUES (seq_room_id.NEXTVAL, 'Unauthorized Room', 1, 2)", 1L)
        );
        assertFalse(insertResp.isSuccess(), "Foreign home INSERT must be rejected");
        assertTrue(insertResp.getMessage().toLowerCase().contains("access denied"));

        // 2. Attempt UPDATE device in Home 2 (Device 10 is in Room 6 / Home 2)
        SqlConsoleResponse updateResp = sqlConsoleService.executeQuery(
                new SqlConsoleRequest("UPDATE devices SET status = 'OFFLINE' WHERE device_id = 10", 1L)
        );
        assertFalse(updateResp.isSuccess(), "Foreign device UPDATE must be rejected");
        assertTrue(updateResp.getMessage().toLowerCase().contains("access denied"));
    }

    @Test
    void testPlSqlFunctionUptime() {
        authenticateAs(normalUser); // Device 1 belongs to Home 1 (accessible)
        SqlConsoleResponse resp = sqlConsoleService.executePlSql(
                new PlSqlExecuteRequest("FN_CALCULATE_DEVICE_UPTIME", Map.of("deviceId", 1L))
        );
        assertTrue(resp.isSuccess(), "Function FN_CALCULATE_DEVICE_UPTIME should succeed");
        assertNotNull(resp.getRows());
        assertEquals(1, resp.getRowCount());
        assertTrue(resp.getRows().get(0).get("UPTIME").toString().contains("days"));
    }

    @Test
    void testPlSqlFunctionCheckDeviceAccess() {
        authenticateAs(normalUser); // User 4
        SqlConsoleResponse resp = sqlConsoleService.executePlSql(
                new PlSqlExecuteRequest("FN_CHECK_DEVICE_ACCESS", Map.of("deviceId", 1L))
        );
        assertTrue(resp.isSuccess(), "Function FN_CHECK_DEVICE_ACCESS should succeed");
        assertEquals("1 (TRUE)", resp.getRows().get(0).get("HAS_ACCESS"));

        // Device 10 belongs to Home 2 -> should return 0 (FALSE)
        SqlConsoleResponse foreignResp = sqlConsoleService.executePlSql(
                new PlSqlExecuteRequest("FN_CHECK_DEVICE_ACCESS", Map.of("deviceId", 10L))
        );
        assertTrue(foreignResp.isSuccess());
        assertEquals("0 (FALSE)", foreignResp.getRows().get(0).get("HAS_ACCESS"));
    }

    @Test
    void testPlSqlProcedureHealthReport() {
        authenticateAs(normalUser); // Home 1 accessible
        SqlConsoleResponse resp = sqlConsoleService.executePlSql(
                new PlSqlExecuteRequest("SP_HOME_DEVICE_HEALTH_REPORT", Map.of("homeId", 1L))
        );
        assertTrue(resp.isSuccess(), "Health report procedure should succeed: " + resp.getMessage());
        assertNotNull(resp.getRows());
        assertTrue(resp.getRowCount() > 3, "Report must contain formatted output lines");

        // Attempt Home 2 -> access denied
        SqlConsoleResponse foreignResp = sqlConsoleService.executePlSql(
                new PlSqlExecuteRequest("SP_HOME_DEVICE_HEALTH_REPORT", Map.of("homeId", 2L))
        );
        assertFalse(foreignResp.isSuccess(), "Foreign home health report must be denied");
    }

    @Test
    void testPlSqlProcedureRecordSensorReadingAndCleanup() {
        authenticateAs(normalUser); // Device 1
        SqlConsoleResponse resp = sqlConsoleService.executePlSql(
                new PlSqlExecuteRequest("SP_RECORD_SENSOR_READING", Map.of("deviceId", 1L, "value", 25.75))
        );
        assertTrue(resp.isSuccess(), "Procedure SP_RECORD_SENSOR_READING should succeed");
        assertTrue(resp.getMessage().contains("Recorded sensor reading"));

        // Clean up created reading for device 1 where value = 25.75
        jdbcTemplate.update("DELETE FROM sensor_readings WHERE device_id = 1 AND value = 25.75");
    }

    @Test
    void testPlSqlTriggerTestCyclePrevention() {
        authenticateAs(normalUser);
        SqlConsoleResponse resp = sqlConsoleService.executePlSql(
                new PlSqlExecuteRequest("TEST_TRIGGER_CYCLE", Map.of("deviceId", 1L))
        );
        assertTrue(resp.isSuccess(), "Trigger test should complete successfully");
        assertTrue(resp.getMessage().contains("TRG_PREVENT_DEVICE_CYCLE"));
        assertTrue(resp.getMessage().contains("ORA-20030"));
    }

    @Test
    void testUnapprovedPlSqlRoutineRejected() {
        authenticateAs(adminUser);
        SqlConsoleResponse resp = sqlConsoleService.executePlSql(
                new PlSqlExecuteRequest("DBMS_UTILITY.EXEC_DDL_STATEMENT", Map.of("cmd", "DROP TABLE devices"))
        );
        assertFalse(resp.isSuccess(), "Unapproved stored routines must be rejected");
        assertTrue(resp.getMessage().toLowerCase().contains("access denied") || resp.getMessage().toLowerCase().contains("allowlist"));
    }
}
