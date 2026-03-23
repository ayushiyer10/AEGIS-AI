package com.anticheat.agent;

public class MouseStats {

    private volatile int dx = 0;
    private volatile int dy = 0;
    private volatile int clicks = 0;

    public void start() {
        // PSEUDO: integrate JNativeHook or JNA here
        // onMouseMove(dx, dy)
        // onMouseClick()
    }

    public synchronized String snapshot() {
        String data = dx + "," + dy + "," + clicks;
        dx = dy = clicks = 0;
        return data;
    }
}
