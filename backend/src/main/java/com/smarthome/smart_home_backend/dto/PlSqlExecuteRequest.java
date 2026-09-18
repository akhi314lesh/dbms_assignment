package com.smarthome.smart_home_backend.dto;

import java.util.Map;

public class PlSqlExecuteRequest {
    private String routineName;
    private Map<String, Object> params;

    public PlSqlExecuteRequest() {
    }

    public PlSqlExecuteRequest(String routineName, Map<String, Object> params) {
        this.routineName = routineName;
        this.params = params;
    }

    public String getRoutineName() {
        return routineName;
    }

    public void setRoutineName(String routineName) {
        this.routineName = routineName;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
