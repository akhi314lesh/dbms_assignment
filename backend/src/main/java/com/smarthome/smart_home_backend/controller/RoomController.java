package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Room;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final HomeAuthorizationService authService;

    public RoomController(RoomService roomService, HomeAuthorizationService authService) {
        this.roomService = roomService;
        this.authService = authService;
    }

    @GetMapping
    public List<Room> getAllRooms() {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return roomService.getAllRooms();
        }
        return roomService.getRoomsByHomeIds(authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        authService.assertCanAccessRoom(id);
        return roomService.getRoomById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/home/{homeId}")
    public List<Room> getRoomsByHome(@PathVariable Long homeId) {
        authService.assertCanAccessHome(homeId);
        return roomService.getRoomsByHomeId(homeId);
    }

    @PostMapping("/home/{homeId}")
    public ResponseEntity<Room> createRoom(
            @PathVariable Long homeId,
            @RequestBody Room room) {

        authService.assertCanManageHome(homeId);
        try {
            Room savedRoom = roomService.createRoom(homeId, room);
            return ResponseEntity.ok(savedRoom);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @RequestBody Room roomDetails) {

        authService.assertCanManageRoom(id);
        try {
            Room updatedRoom = roomService.updateRoom(id, roomDetails);
            return ResponseEntity.ok(updatedRoom);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        authService.assertCanManageRoom(id);
        if (roomService.getRoomById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}