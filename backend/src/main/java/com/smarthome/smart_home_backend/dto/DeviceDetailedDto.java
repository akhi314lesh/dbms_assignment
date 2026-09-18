package com.smarthome.smart_home_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DeviceDetailedDto {
    private Long deviceId;
    private String deviceName;
    private String deviceSubtype;
    private String status;
    private LocalDate installDate;
    private LocalDateTime lastRestartTime;
    private Long roomId;
    private String roomName;
    private Integer floorNumber;
    private Long homeId;
    private String homeName;
    private Long parentDeviceId;
    private String parentDeviceName;
    private String uptime;

    // Smart Light
    private Double brightness;
    private String colorSupport;

    // Thermostat
    private Double targetTemperature;
    private String thermostatMode;

    // Temperature Sensor
    private String unit;
    private Double minRange;
    private Double maxRange;
    private Double latestReadingValue;
    private LocalDateTime latestReadingTime;

    // Motion Sensor
    private String sensitivityLevel;
    private Double detectionRange;

    // Camera
    private String resolution;
    private String storageType;
    private String nightVisionSupport;

    public DeviceDetailedDto() {
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceSubtype() {
        return deviceSubtype;
    }

    public void setDeviceSubtype(String deviceSubtype) {
        this.deviceSubtype = deviceSubtype;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getInstallDate() {
        return installDate;
    }

    public void setInstallDate(LocalDate installDate) {
        this.installDate = installDate;
    }

    public LocalDateTime getLastRestartTime() {
        return lastRestartTime;
    }

    public void setLastRestartTime(LocalDateTime lastRestartTime) {
        this.lastRestartTime = lastRestartTime;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public Long getHomeId() {
        return homeId;
    }

    public void setHomeId(Long homeId) {
        this.homeId = homeId;
    }

    public String getHomeName() {
        return homeName;
    }

    public void setHomeName(String homeName) {
        this.homeName = homeName;
    }

    public Long getParentDeviceId() {
        return parentDeviceId;
    }

    public void setParentDeviceId(Long parentDeviceId) {
        this.parentDeviceId = parentDeviceId;
    }

    public String getParentDeviceName() {
        return parentDeviceName;
    }

    public void setParentDeviceName(String parentDeviceName) {
        this.parentDeviceName = parentDeviceName;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public Double getBrightness() {
        return brightness;
    }

    public void setBrightness(Double brightness) {
        this.brightness = brightness;
    }

    public String getColorSupport() {
        return colorSupport;
    }

    public void setColorSupport(String colorSupport) {
        this.colorSupport = colorSupport;
    }

    public Double getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(Double targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public String getThermostatMode() {
        return thermostatMode;
    }

    public void setThermostatMode(String thermostatMode) {
        this.thermostatMode = thermostatMode;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getMinRange() {
        return minRange;
    }

    public void setMinRange(Double minRange) {
        this.minRange = minRange;
    }

    public Double getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(Double maxRange) {
        this.maxRange = maxRange;
    }

    public Double getLatestReadingValue() {
        return latestReadingValue;
    }

    public void setLatestReadingValue(Double latestReadingValue) {
        this.latestReadingValue = latestReadingValue;
    }

    public LocalDateTime getLatestReadingTime() {
        return latestReadingTime;
    }

    public void setLatestReadingTime(LocalDateTime latestReadingTime) {
        this.latestReadingTime = latestReadingTime;
    }

    public String getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(String sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public Double getDetectionRange() {
        return detectionRange;
    }

    public void setDetectionRange(Double detectionRange) {
        this.detectionRange = detectionRange;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getNightVisionSupport() {
        return nightVisionSupport;
    }

    public void setNightVisionSupport(String nightVisionSupport) {
        this.nightVisionSupport = nightVisionSupport;
    }
}
