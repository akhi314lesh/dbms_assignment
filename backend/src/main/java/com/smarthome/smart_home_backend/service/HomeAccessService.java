package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Home;
import com.smarthome.smart_home_backend.entity.HomeAccess;
import com.smarthome.smart_home_backend.entity.HomeAccessId;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.HomeAccessRepository;
import com.smarthome.smart_home_backend.repository.HomeRepository;
import com.smarthome.smart_home_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HomeAccessService {

    private final HomeAccessRepository homeAccessRepository;
    private final UserRepository userRepository;
    private final HomeRepository homeRepository;

    public HomeAccessService(
            HomeAccessRepository homeAccessRepository,
            UserRepository userRepository,
            HomeRepository homeRepository) {

        this.homeAccessRepository = homeAccessRepository;
        this.userRepository = userRepository;
        this.homeRepository = homeRepository;
    }

    public List<HomeAccess> getAccessByUserId(Long userId) {
        return homeAccessRepository.findByUserUserId(userId);
    }

    public List<HomeAccess> getAccessByHomeId(Long homeId) {
        return homeAccessRepository.findByHomeHomeId(homeId);
    }

    public Optional<HomeAccess> getAccess(
            Long userId,
            Long homeId) {

        HomeAccessId id = new HomeAccessId(userId, homeId);

        return homeAccessRepository.findById(id);
    }

    public HomeAccess createAccess(
            Long userId,
            Long homeId,
            HomeAccess access) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Home home = homeRepository.findById(homeId)
                .orElseThrow(() ->
                        new RuntimeException("Home not found"));

        HomeAccessId id =
                new HomeAccessId(userId, homeId);

        access.setId(id);
        access.setUser(user);
        access.setHome(home);

        return homeAccessRepository.save(access);
    }

    public void deleteAccess(
            Long userId,
            Long homeId) {

        HomeAccessId id =
                new HomeAccessId(userId, homeId);

        homeAccessRepository.deleteById(id);
    }
}