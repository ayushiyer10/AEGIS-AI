package com.anticheat.agent;

public class AgentApplication {
    public static void main(String[] args) {
        InputCollector collector = new InputCollector();
        collector.start();
        System.out.println("AntiCheat Agent running (no UI)");
    }
}
