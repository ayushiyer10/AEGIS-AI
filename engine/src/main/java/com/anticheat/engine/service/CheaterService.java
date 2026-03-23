package com.anticheat.engine.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

@Service
public class CheaterService {

    // ================================
    // STORAGE
    // ================================
    private final List<Map<String, Object>> mouseData = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> keyboardData = new CopyOnWriteArrayList<>();

    private double currentVelocity = 0;
    private boolean isCheater = false;
    private int totalCheaters = 0;

    // ================================
    // GETTERS
    // ================================
    public List<Map<String, Object>> getMouseData() {
        return mouseData;
    }

    public List<Map<String, Object>> getKeyboardData() {
        return keyboardData;
    }

    public double getCurrentVelocity() {
        return currentVelocity;
    }

    public boolean isCheater() {
        return isCheater;
    }

    public int getTotalCheaters() {
        return totalCheaters;
    }

    // ================================
    // UPDATE FROM PYTHON AGENT
    // ================================
    public void updateMouse(int x, int y, double t) {

        Map<String, Object> m = new HashMap<>();
        m.put("x", x);
        m.put("y", y);
        m.put("t", t);

        mouseData.add(m);

        // keep last 200 points only
        if (mouseData.size() > 200)
            mouseData.remove(0);
    }

    public void updateKeyboard(String key, double t) {

        Map<String, Object> k = new HashMap<>();
        k.put("key", key);
        k.put("t", t);

        keyboardData.add(k);

        if (keyboardData.size() > 200)
            keyboardData.remove(0);
    }

    public void updateStats(double velocity, boolean cheater) {

        this.currentVelocity = velocity;

        if (cheater && !this.isCheater)
            totalCheaters++;

        this.isCheater = cheater;
    }
}
