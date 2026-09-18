package com.smarthome.smart_home_backend.dto;

public class SqlConsoleRequest {
    private String sql;
    private Long homeId;

    public SqlConsoleRequest() {
    }

    public SqlConsoleRequest(String sql, Long homeId) {
        this.sql = sql;
        this.homeId = homeId;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Long getHomeId() {
        return homeId;
    }

    public void setHomeId(Long homeId) {
        this.homeId = homeId;
    }
}
