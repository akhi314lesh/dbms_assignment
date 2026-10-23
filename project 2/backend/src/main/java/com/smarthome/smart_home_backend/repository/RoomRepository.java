package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHomeHomeId(Long homeId);

    List<Room> findByHomeHomeIdIn(Collection<Long> homeIds);
}