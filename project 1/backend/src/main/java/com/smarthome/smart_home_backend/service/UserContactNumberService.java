package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.entity.UserContactNumber;
import com.smarthome.smart_home_backend.entity.UserContactNumberId;
import com.smarthome.smart_home_backend.repository.UserContactNumberRepository;
import com.smarthome.smart_home_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserContactNumberService {

    private final UserContactNumberRepository repository;
    private final UserRepository userRepository;

    public UserContactNumberService(
            UserContactNumberRepository repository,
            UserRepository userRepository) {

        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<UserContactNumber> getContactsByUserId(Long userId) {
        return repository.findByUserUserId(userId);
    }

    public Optional<UserContactNumber> getContact(
            Long userId,
            String contactNumber) {

        UserContactNumberId id =
                new UserContactNumberId(userId, contactNumber);

        return repository.findById(id);
    }

    @Transactional
    public UserContactNumber createContact(
            Long userId,
            UserContactNumber contact) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (contact.getId() == null) {
            contact.setId(new UserContactNumberId());
        }

        contact.getId().setUserId(userId);
        contact.setUser(user);

        return repository.save(contact);
    }

    @Transactional
    public void deleteContact(
            Long userId,
            String contactNumber) {

        UserContactNumberId id =
                new UserContactNumberId(userId, contactNumber);

        repository.deleteById(id);
    }
}