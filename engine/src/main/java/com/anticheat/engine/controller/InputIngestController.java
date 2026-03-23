package com.anticheat.engine.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anticheat.engine.service.LiveDataStore;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class InputIngestController {

    @PostMapping("/ingest")
    public Map<String, String> ingest(@RequestBody Map<String, Object> payload) {
        LiveDataStore.update(payload);
        return Map.of("status", "received");
    }
}
