package com.anticheat.engine.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anticheat.engine.service.CheaterService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins="*")
public class InputController {

    private final CheaterService service;

    public InputController(CheaterService service){
        this.service = service;
    }

    @PostMapping("/updateMouse")
    public void mouse(@RequestParam int x,
                      @RequestParam int y,
                      @RequestParam double t){
        service.updateMouse(x,y,t);
    }

    @PostMapping("/updateKeyboard")
    public void keyboard(@RequestParam String key,
                         @RequestParam double t){
        service.updateKeyboard(key,t);
    }

    @PostMapping("/updateStats")
    public void stats(@RequestParam double velocity,
                      @RequestParam boolean cheater){
        service.updateStats(velocity,cheater);
    }
}
