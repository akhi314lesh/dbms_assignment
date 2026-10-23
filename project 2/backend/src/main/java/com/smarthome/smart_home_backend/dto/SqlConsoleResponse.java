package com.smarthome.smart_home_backend.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SqlConsoleResponse {
    private boolean success;
    private String message;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private int rowCount;
    private int affectedRows;
    private long executionTimeMs;
    private String executedSql;

    public SqlConsoleResponse() {
        this.columns = Collections.emptyList();
        this.rows = Collections.emptyList();
    }

    public static SqlConsoleResponse querySuccess(List<String> columns, List<Map<String, Object>> rows, long executionTimeMs, String executedSql) {
        SqlConsoleResponse resp = new SqlConsoleResponse();
        resp.setSuccess(true);
        resp.setMessage("Query executed successfully.");
        resp.setColumns(columns);
        resp.setRows(rows);
        resp.setRowCount(rows != null ? rows.size() : 0);
        resp.setAffectedRows(0);
        resp.setExecutionTimeMs(executionTimeMs);
        resp.setExecutedSql(executedSql);
        return resp;
    }

    public static SqlConsoleResponse dmlSuccess(int affectedRows, long executionTimeMs, String executedSql) {
        SqlConsoleResponse resp = new SqlConsoleResponse();
        resp.setSuccess(true);
        resp.setMessage(affectedRows + " row(s) affected.");
        resp.setColumns(Collections.emptyList());
        resp.setRows(Collections.emptyList());
        resp.setRowCount(0);
        resp.setAffectedRows(affectedRows);
        resp.setExecutionTimeMs(executionTimeMs);
        resp.setExecutedSql(executedSql);
        return resp;
    }

    public static SqlConsoleResponse error(String message, long executionTimeMs) {
        SqlConsoleResponse resp = new SqlConsoleResponse();
        resp.setSuccess(false);
        resp.setMessage(message);
        resp.setColumns(Collections.emptyList());
        resp.setRows(Collections.emptyList());
        resp.setRowCount(0);
        resp.setAffectedRows(0);
        resp.setExecutionTimeMs(executionTimeMs);
        return resp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, Object>> rows) {
        this.rows = rows;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getExecutedSql() {
        return executedSql;
    }

    public void setExecutedSql(String executedSql) {
        this.executedSql = executedSql;
    }
}
