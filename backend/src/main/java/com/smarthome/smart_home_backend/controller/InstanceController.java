package com.smarthome.smart_home_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/instance")
public class InstanceController {

    @Value("${INSTANCE_NAME:local-default}")
    private String instanceName;

    @GetMapping
    public Map<String, String> getInstance() {
        return Map.of("instance", instanceName);
    }
}
