package com.anticheat.engine;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anticheat.engine.service.LiveDataStore;

@SpringBootTest
@AutoConfigureMockMvc
class LiveApiRegressionTests {

    @Autowired
    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        System.setProperty("engine.live.snapshot.path", tempDir.resolve("live-snapshots.jsonl").toString());
        LiveDataStore.resetForTests();
    }

    @Test
    void ingestUpdatesLivePayloadWithExpectedFields() throws Exception {
        String payload = """
                {
                  "deviceId": "test-device-001",
                  "mouse": [
                    { "x": 100, "y": 100, "t": 1.00 },
                    { "x": 160, "y": 120, "t": 1.05 },
                    { "x": 260, "y": 150, "t": 1.10 },
                    { "x": 380, "y": 180, "t": 1.15 }
                  ],
                  "keys": [
                    { "key": "w", "t": 1.12 }
                  ],
                  "timestamp": 1.20
                }
                """;

        mockMvc.perform(post("/api/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("test-device-001"))
                .andExpect(jsonPath("$.mouse").isArray())
                .andExpect(jsonPath("$.keyboard").isArray())
                .andExpect(jsonPath("$.velocity").isNumber())
                .andExpect(jsonPath("$.velocityAvg").isNumber())
                .andExpect(jsonPath("$.velocitySmoothed").isNumber())
                .andExpect(jsonPath("$.cheatScore").isNumber())
                .andExpect(jsonPath("$.isCheater").isBoolean())
                .andExpect(jsonPath("$.velocity").value(org.hamcrest.Matchers.greaterThan(0.0)));
    }

    @Test
    void snapshotsEndpointReturnsPersistedLiveSnapshots() throws Exception {
        String payloadA = """
                {
                  "mouse": [
                    { "x": 0, "y": 0, "t": 1.00 },
                    { "x": 50, "y": 40, "t": 1.05 }
                  ],
                  "keys": [],
                  "timestamp": 1.20
                }
                """;

        String payloadB = """
                {
                  "mouse": [
                    { "x": 50, "y": 40, "t": 1.10 },
                    { "x": 120, "y": 90, "t": 1.15 }
                  ],
                  "keys": [],
                  "timestamp": 1.30
                }
                """;

        mockMvc.perform(post("/api/ingest").contentType(MediaType.APPLICATION_JSON).content(payloadA))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ingest").contentType(MediaType.APPLICATION_JSON).content(payloadB))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/live/snapshots").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].velocity").isNumber())
                .andExpect(jsonPath("$[0].mouse").isArray())
                .andExpect(jsonPath("$[0].keyboard").isArray());
    }

    @Test
    void registerHeartbeatAndDeviceListEndpointsWork() throws Exception {
        String registration = """
                {
                  "deviceId": "device-laptop-01",
                  "hostname": "Laptop-01",
                  "os": "Windows",
                  "appVersion": "1.0.0",
                  "ip": "192.168.1.100"
                }
                """;

        mockMvc.perform(post("/api/agents/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("registered"))
                .andExpect(jsonPath("$.deviceId").value("device-laptop-01"));

        mockMvc.perform(post("/api/agents/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("alive"));

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].deviceId").value("device-laptop-01"))
                .andExpect(jsonPath("$[0].online").isBoolean());
    }
}
