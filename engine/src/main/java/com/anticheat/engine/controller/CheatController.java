package com.anticheat.engine.controller;


import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anticheat.engine.model.CheaterStats;
import com.anticheat.engine.service.CheaterService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins="*")
public class CheatController {

    private final CheaterService cheaterService;

    public CheatController(CheaterService cheaterService) {
        this.cheaterService = cheaterService;
    }

    @GetMapping("/health")
    public String health() {
        return "ENGINE_OK";
    }

    @GetMapping("/stats")
    public CheaterStats stats() {
        return new CheaterStats(
            cheaterService.getTotalCheaters(),
            "ONLINE"
        );
    }
}
