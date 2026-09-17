package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Home;
import com.smarthome.smart_home_backend.repository.HomeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HomeService {

    private final HomeRepository homeRepository;

    public HomeService(HomeRepository homeRepository) {
        this.homeRepository = homeRepository;
    }

    public List<Home> getAllHomes() {
        return homeRepository.findAll();
    }

    public Optional<Home> getHomeById(Long id) {
        return homeRepository.findById(id);
    }

    public Home createHome(Home home) {
        return homeRepository.save(home);
    }

    public Home updateHome(Long id, Home homeDetails) {

        Home home = homeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Home not found"));

        home.setHomeName(homeDetails.getHomeName());
        home.setStreet(homeDetails.getStreet());
        home.setCity(homeDetails.getCity());
        home.setPincode(homeDetails.getPincode());

        return homeRepository.save(home);
    }

    public void deleteHome(Long id) {
        homeRepository.deleteById(id);
    }
}