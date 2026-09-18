package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.HomeAccess;
import com.smarthome.smart_home_backend.entity.HomeAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HomeAccessRepository
        extends JpaRepository<HomeAccess, HomeAccessId> {

    List<HomeAccess> findByUserUserId(Long userId);

    List<HomeAccess> findByHomeHomeId(Long homeId);

    @Query("SELECT ha.home.homeId FROM HomeAccess ha WHERE ha.user.userId = :userId")
    List<Long> findHomeIdsByUserId(@Param("userId") Long userId);

    Optional<HomeAccess> findByUserUserIdAndHomeHomeId(Long userId, Long homeId);

    boolean existsByUserUserIdAndHomeHomeId(Long userId, Long homeId);
}