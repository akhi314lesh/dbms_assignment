package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Home;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface HomeRepository extends JpaRepository<Home, Long> {

    List<Home> findByHomeIdIn(Collection<Long> homeIds);

    @Query("SELECT h.homeId FROM Home h")
    List<Long> findAllHomeIds();
}