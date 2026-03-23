package com.anticheat.engine.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anticheat.engine.service.LiveDataStore;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentController {

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> payload) {
        LiveDataStore.registerAgent(payload);
        return Map.of(
                "status", "registered",
                "deviceId", payload.getOrDefault("deviceId", "unknown")
        );
    }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat(@RequestBody Map<String, Object> payload) {
        LiveDataStore.heartbeat(payload);
        return Map.of(
                "status", "alive",
                "deviceId", payload.getOrDefault("deviceId", "unknown")
        );
    }
}
