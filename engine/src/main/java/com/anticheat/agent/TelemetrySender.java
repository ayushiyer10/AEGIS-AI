package com.anticheat.agent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelemetrySender {

    public void send(String mouse, int keys) {
        try {
            URL url = new URL("http://localhost:9000/ingest");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            String json = """
            {
              "mouse": "%s",
              "keys": %d,
              "timestamp": %d
            }
            """.formatted(mouse, keys, System.currentTimeMillis());

            try (OutputStream os = con.getOutputStream()) {
                os.write(json.getBytes());
            }

            con.getResponseCode();
        } catch (Exception ignored) {}
    }
}
