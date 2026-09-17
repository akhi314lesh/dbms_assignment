package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.UserContactNumber;
import com.smarthome.smart_home_backend.entity.UserContactNumberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserContactNumberRepository
        extends JpaRepository<UserContactNumber, UserContactNumberId> {

    List<UserContactNumber> findByUserUserId(Long userId);
}