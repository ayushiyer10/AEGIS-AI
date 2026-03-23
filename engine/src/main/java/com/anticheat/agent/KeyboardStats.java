package com.anticheat.agent;

public class KeyboardStats {

    private volatile int keyCount = 0;

    public void start() {
        // PSEUDO: onKeyPress → keyCount++
    }

    public synchronized int snapshot() {
        int count = keyCount;
        keyCount = 0;
        return count;
    }
}
