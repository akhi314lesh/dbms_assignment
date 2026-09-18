package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Camera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CameraRepository
        extends JpaRepository<Camera, Long> {

    @Query("SELECT c FROM Camera c WHERE c.device.room.home.homeId IN :homeIds")
    List<Camera> findByHomeIds(@Param("homeIds") Collection<Long> homeIds);
}