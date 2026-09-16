package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Home;
import com.smarthome.smart_home_backend.entity.Room;
import com.smarthome.smart_home_backend.repository.HomeRepository;
import com.smarthome.smart_home_backend.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final HomeRepository homeRepository;

    public RoomService(
            RoomRepository roomRepository,
            HomeRepository homeRepository) {

        this.roomRepository = roomRepository;
        this.homeRepository = homeRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public List<Room> getRoomsByHomeId(Long homeId) {
        return roomRepository.findByHomeHomeId(homeId);
    }

    public Room createRoom(Long homeId, Room room) {

        Home home = homeRepository.findById(homeId)
                .orElseThrow(() ->
                        new RuntimeException("Home not found"));

        room.setHome(home);

        return roomRepository.save(room);
    }

    public Room updateRoom(
            Long id,
            Room roomDetails) {

        Room room = roomRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Room not found"));

        room.setRoomName(roomDetails.getRoomName());
        room.setFloorNumber(roomDetails.getFloorNumber());

        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }
}