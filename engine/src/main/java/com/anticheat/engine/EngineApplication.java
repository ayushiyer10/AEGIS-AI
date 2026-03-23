package com.anticheat.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javafx.application.Application;

@SpringBootApplication
public class EngineApplication {

    public static void main(String[] args) {

        // Start Spring Boot backend in background
        new Thread(() -> SpringApplication.run(EngineApplication.class, args)).start();

        // Start JavaFX desktop window
        Application.launch(DesktopWindow.class);
    }
}
