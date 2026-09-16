package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Home;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeRepository extends JpaRepository<Home, Long> {
}