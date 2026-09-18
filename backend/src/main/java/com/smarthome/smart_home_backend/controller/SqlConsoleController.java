package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.dto.PlSqlExecuteRequest;
import com.smarthome.smart_home_backend.dto.SqlConsoleRequest;
import com.smarthome.smart_home_backend.dto.SqlConsoleResponse;
import com.smarthome.smart_home_backend.service.SqlConsoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-console")
public class SqlConsoleController {

    private final SqlConsoleService sqlConsoleService;

    public SqlConsoleController(SqlConsoleService sqlConsoleService) {
        this.sqlConsoleService = sqlConsoleService;
    }

    @PostMapping("/execute")
    public ResponseEntity<SqlConsoleResponse> executeQuery(@RequestBody SqlConsoleRequest request) {
        SqlConsoleResponse response = sqlConsoleService.executeQuery(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute-plsql")
    public ResponseEntity<SqlConsoleResponse> executePlSql(@RequestBody PlSqlExecuteRequest request) {
        SqlConsoleResponse response = sqlConsoleService.executePlSql(request);
        return ResponseEntity.ok(response);
    }
}
