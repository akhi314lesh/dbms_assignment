package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "HOMES")
public class Home {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "home_seq")
    @SequenceGenerator(
            name = "home_seq",
            sequenceName = "SEQ_HOME_ID",
            allocationSize = 1
    )
    @Column(name = "home_id")
    private Long homeId;

    @Column(name = "home_name", nullable = false, length = 100)
    private String homeName;

    @Column(name = "street", length = 150)
    private String street;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "pincode", length = 10)
    private String pincode;

    public Home() {
    }

    public Home(String homeName, String street, String city, String pincode) {
        this.homeName = homeName;
        this.street = street;
        this.city = city;
        this.pincode = pincode;
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

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }
}