package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.HomeAccess;
import com.smarthome.smart_home_backend.entity.HomeAccessId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeAccessRepository
        extends JpaRepository<HomeAccess, HomeAccessId> {

    List<HomeAccess> findByUserUserId(Long userId);

    List<HomeAccess> findByHomeHomeId(Long homeId);
}