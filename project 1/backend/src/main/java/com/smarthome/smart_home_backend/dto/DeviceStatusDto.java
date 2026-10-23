package com.smarthome.smart_home_backend.dto;

public class DeviceStatusDto {

    private Long deviceId;
    private String status;

    public DeviceStatusDto() {}

    public DeviceStatusDto(Long deviceId, String status) {
        this.deviceId = deviceId;
        this.status = status;
    }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
