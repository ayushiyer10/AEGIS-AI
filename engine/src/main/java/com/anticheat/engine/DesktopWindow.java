package com.anticheat.engine;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class DesktopWindow extends Application {

    private static final String APP_URL = "http://localhost:8080";
    private static final String HEALTH_URL = "http://localhost:8080/api/health";

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        webView.getEngine().loadContent("""
            <html>
              <body style="margin:0;background:#0b1220;color:#dbeafe;font-family:Segoe UI,Arial,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;">
                <div>
                  <h2 style="margin:0 0 12px 0;">Starting AEGIS AI...</h2>
                  <p style="margin:0;opacity:.8;">Waiting for engine at http://localhost:8080</p>
                </div>
              </body>
            </html>
            """);

        waitForBackendAndLoad(webView);

        Scene scene = new Scene(webView, 1280, 800);
        stage.setTitle("AEGIS AI - Anti-Cheat Engine");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        scheduler.shutdownNow();
    }

    private void waitForBackendAndLoad(WebView webView) {
        final int maxAttempts = 30;
        final int[] attempts = {0};

        scheduler.scheduleAtFixedRate(() -> {
            attempts[0]++;
            if (isBackendReady()) {
                Platform.runLater(() -> webView.getEngine().load(buildDesktopUrl()));
                scheduler.shutdown();
                return;
            }

            if (attempts[0] >= maxAttempts) {
                Platform.runLater(() -> webView.getEngine().loadContent("""
                    <html>
                      <body style="margin:0;background:#1f2937;color:#f9fafb;font-family:Segoe UI,Arial,sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;">
                        <div>
                          <h2 style="margin:0 0 12px 0;">Engine Failed To Start</h2>
                          <p style="margin:0 0 8px 0;">Could not reach http://localhost:8080 within 30 seconds.</p>
                          <p style="margin:0;opacity:.8;">Check backend logs and restart the app.</p>
                        </div>
                      </body>
                    </html>
                    """));
                scheduler.shutdown();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private boolean isBackendReady() {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(HEALTH_URL).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(700);
            conn.setReadTimeout(700);
            int status = conn.getResponseCode();
            return status >= 200 && status < 300;
        } catch (IOException ignored) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String buildDesktopUrl() {
        return APP_URL + "?desktop=1&ts=" + System.currentTimeMillis();
    }
}
