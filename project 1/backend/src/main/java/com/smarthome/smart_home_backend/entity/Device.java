package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "DEVICES")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "device_seq")
    @SequenceGenerator(
            name = "device_seq",
            sequenceName = "SEQ_DEVICE_ID",
            allocationSize = 1
    )
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    @Column(name = "device_subtype", nullable = false, length = 50)
    private String deviceSubtype;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "install_date")
    private LocalDate installDate;

    @Column(name = "last_restart_time")
    private LocalDateTime lastRestartTime;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name = "parent_device_id")
    private Device parentDevice;

    public Device() {
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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Device getParentDevice() {
        return parentDevice;
    }

    public void setParentDevice(Device parentDevice) {
        this.parentDevice = parentDevice;
    }
}