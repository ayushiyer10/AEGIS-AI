package com.anticheat.engine.controller;

import java.util.Map;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anticheat.engine.service.LiveDataStore;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins="*")
public class StatsController {

    @GetMapping("/live")
    public Map<String,Object> getLive(@RequestParam(required = false) String deviceId){
        if (deviceId == null || deviceId.isBlank()) {
            return LiveDataStore.getLiveState();
        }
        return LiveDataStore.getLiveState(deviceId);
    }

    @GetMapping("/live/snapshots")
    public List<Map<String, Object>> getLiveSnapshots(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String deviceId){
        if (deviceId == null || deviceId.isBlank()) {
            return LiveDataStore.getSnapshots(limit);
        }
        return LiveDataStore.getSnapshots(deviceId, limit);
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> getDevices() {
        return LiveDataStore.getDevices();
    }

    @GetMapping("/devices/{deviceId}/live")
    public Map<String, Object> getDeviceLive(@PathVariable String deviceId) {
        return LiveDataStore.getLiveState(deviceId);
    }

    @GetMapping("/devices/{deviceId}/snapshots")
    public List<Map<String, Object>> getDeviceSnapshots(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "50") int limit) {
        return LiveDataStore.getSnapshots(deviceId, limit);
    }
}
