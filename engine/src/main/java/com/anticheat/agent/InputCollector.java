package com.anticheat.agent;

import java.util.Timer;
import java.util.TimerTask;

public class InputCollector {

    private final MouseStats mouse = new MouseStats();
    private final KeyboardStats keyboard = new KeyboardStats();
    private final TelemetrySender sender = new TelemetrySender();

    public void start() {
        mouse.start();
        keyboard.start();

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sender.send(mouse.snapshot(), keyboard.snapshot());
            }
        }, 0, 200); // every 200 ms
    }
}
