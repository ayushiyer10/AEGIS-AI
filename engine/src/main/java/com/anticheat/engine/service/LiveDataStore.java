package com.anticheat.engine.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LiveDataStore {

    private static final Object SNAPSHOT_FILE_LOCK = new Object();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Pattern DEVICE_SAFE_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");

    private static final int OFFLINE_TIMEOUT_MS = 15_000;

    private static final ConcurrentHashMap<String, DeviceState> DEVICE_STATES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AgentPresence> AGENT_PRESENCE = new ConcurrentHashMap<>();
    private static final AtomicReference<String> LATEST_DEVICE_ID = new AtomicReference<>("unknown");

    private LiveDataStore() {
    }

    @SuppressWarnings("unchecked")
    public static void update(Map<String, Object> data) {
        Map<String, Object> safeData = data != null ? data : Collections.emptyMap();
        String deviceId = extractDeviceId(safeData);
        LATEST_DEVICE_ID.set(deviceId);

        DeviceState state = DEVICE_STATES.computeIfAbsent(deviceId, DeviceState::new);
        state.update(safeData);

        AGENT_PRESENCE.compute(deviceId, (id, existing) -> {
            AgentPresence presence = existing != null ? existing : new AgentPresence(id);
            presence.touch();
            presence.hostname = safeString(safeData.get("hostname"), presence.hostname);
            presence.os = safeString(safeData.get("os"), presence.os);
            presence.appVersion = safeString(safeData.get("appVersion"), presence.appVersion);
            presence.ip = safeString(safeData.get("ip"), presence.ip);
            return presence;
        });
    }

    public static void registerAgent(Map<String, Object> payload) {
        Map<String, Object> safeData = payload != null ? payload : Collections.emptyMap();
        String deviceId = extractDeviceId(safeData);
        AGENT_PRESENCE.compute(deviceId, (id, existing) -> {
            AgentPresence presence = existing != null ? existing : new AgentPresence(id);
            presence.touch();
            presence.hostname = safeString(safeData.get("hostname"), presence.hostname);
            presence.os = safeString(safeData.get("os"), presence.os);
            presence.appVersion = safeString(safeData.get("appVersion"), presence.appVersion);
            presence.ip = safeString(safeData.get("ip"), presence.ip);
            return presence;
        });
    }

    public static void heartbeat(Map<String, Object> payload) {
        Map<String, Object> safeData = payload != null ? payload : Collections.emptyMap();
        String deviceId = extractDeviceId(safeData);
        AGENT_PRESENCE.compute(deviceId, (id, existing) -> {
            AgentPresence presence = existing != null ? existing : new AgentPresence(id);
            presence.touch();
            presence.hostname = safeString(safeData.get("hostname"), presence.hostname);
            presence.os = safeString(safeData.get("os"), presence.os);
            presence.appVersion = safeString(safeData.get("appVersion"), presence.appVersion);
            presence.ip = safeString(safeData.get("ip"), presence.ip);
            return presence;
        });
    }

    public static Map<String, Object> getLiveState() {
        return getLiveState(LATEST_DEVICE_ID.get());
    }

    public static Map<String, Object> getLiveState(String deviceId) {
        String resolvedDeviceId = sanitizeDeviceId(deviceId);
        DeviceState state = DEVICE_STATES.get(resolvedDeviceId);
        if (state == null) {
            return defaultLiveState(resolvedDeviceId);
        }
        return state.getLiveState();
    }

    public static List<Map<String, Object>> getSnapshots(int limit) {
        return getSnapshots(LATEST_DEVICE_ID.get(), limit);
    }

    public static List<Map<String, Object>> getSnapshots(String deviceId, int limit) {
        String resolvedDeviceId = sanitizeDeviceId(deviceId);
        DeviceState state = DEVICE_STATES.get(resolvedDeviceId);
        if (state == null) {
            return Collections.emptyList();
        }
        return state.getSnapshots(limit);
    }

    public static List<Map<String, Object>> getDevices() {
        List<Map<String, Object>> devices = new ArrayList<>();
        AGENT_PRESENCE.forEach((deviceId, presence) -> {
            DeviceState state = DEVICE_STATES.get(deviceId);
            Map<String, Object> row = new HashMap<>();
            row.put("deviceId", deviceId);
            row.put("hostname", presence.hostname != null ? presence.hostname : "unknown");
            row.put("os", presence.os != null ? presence.os : "unknown");
            row.put("appVersion", presence.appVersion != null ? presence.appVersion : "unknown");
            row.put("ip", presence.ip != null ? presence.ip : "unknown");
            row.put("lastSeen", presence.lastSeenEpochMs);
            row.put("online", presence.isOnline());

            if (state != null) {
                Map<String, Object> live = state.getLiveState();
                row.put("cheatScore", live.getOrDefault("cheatScore", 0.0));
                row.put("isCheater", live.getOrDefault("isCheater", false));
                row.put("velocitySmoothed", live.getOrDefault("velocitySmoothed", 0.0));
                row.put("riskLevel", live.getOrDefault("riskLevel", "clear"));
                row.put("threatLevel", live.getOrDefault("threatLevel", 0.0));
                row.put("suspiciousStreak", live.getOrDefault("suspiciousStreak", 0));
                row.put("evidenceScore", live.getOrDefault("evidenceScore", 0.0));
                row.put("detectionCount", live.getOrDefault("detectionCount", 0));
                row.put("firstDetectedAt", live.getOrDefault("firstDetectedAt", 0L));
                row.put("lastDetectedAt", live.getOrDefault("lastDetectedAt", 0L));
            } else {
                row.put("cheatScore", 0.0);
                row.put("isCheater", false);
                row.put("velocitySmoothed", 0.0);
                row.put("riskLevel", "clear");
                row.put("threatLevel", 0.0);
                row.put("suspiciousStreak", 0);
                row.put("evidenceScore", 0.0);
                row.put("detectionCount", 0);
                row.put("firstDetectedAt", 0L);
                row.put("lastDetectedAt", 0L);
            }

            devices.add(row);
        });

        devices.sort(Comparator.<Map<String, Object>>comparingLong(
                row -> ((Number) row.getOrDefault("lastSeen", 0L)).longValue()
        ).reversed());
        return devices;
    }

    public static void resetForTests() {
        DEVICE_STATES.clear();
        AGENT_PRESENCE.clear();
        LATEST_DEVICE_ID.set("unknown");
    }

    private static Map<String, Object> defaultLiveState(String deviceId) {
        Map<String, Object> out = new HashMap<>();
        out.put("timestamp", Instant.now().toEpochMilli());
        out.put("deviceId", deviceId);
        out.put("mouse", Collections.emptyList());
        out.put("velocity", 0.0);
        out.put("velocityAvg", 0.0);
        out.put("velocitySmoothed", 0.0);
        out.put("keyboard", Collections.emptyList());
        out.put("cheatScore", 0.0);
        out.put("suspiciousStreak", 0);
        out.put("isCheater", false);
        out.put("screenMotion", 0.0);
        out.put("threatLevel", 0.0);
        out.put("threatReason", "none");
        out.put("riskLevel", "clear");
        out.put("evidenceScore", 0.0);
        out.put("modEvidenceStreak", 0);
        out.put("gameActive", false);
        out.put("gameExe", "unknown");
        out.put("online", false);
        out.put("detectionCount", 0);
        out.put("firstDetectedAt", 0L);
        out.put("lastDetectedAt", 0L);
        return out;
    }

    private static String extractDeviceId(Map<String, Object> data) {
        return sanitizeDeviceId(safeString(data.get("deviceId"), "unknown"));
    }

    private static String sanitizeDeviceId(String deviceId) {
        String fallback = deviceId == null || deviceId.isBlank() ? "unknown" : deviceId;
        return DEVICE_SAFE_CHARS.matcher(fallback).replaceAll("_");
    }

    private static String safeString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String s = value.toString().trim();
        if (s.isEmpty()) {
            return fallback;
        }
        return s;
    }

    private static Path resolveSnapshotPath(String deviceId) {
        String override = System.getProperty("engine.live.snapshot.path");
        if (override != null && !override.isBlank()) {
            Path p = Paths.get(override);
            if (Files.isDirectory(p)) {
                return p.resolve(deviceId + ".jsonl");
            }
            return p;
        }
        return Paths.get("data", "live-snapshots", deviceId + ".jsonl");
    }

    private static final class AgentPresence {
        private final String deviceId;
        private volatile long lastSeenEpochMs;
        private volatile String hostname = "unknown";
        private volatile String os = "unknown";
        private volatile String appVersion = "unknown";
        private volatile String ip = "unknown";

        private AgentPresence(String deviceId) {
            this.deviceId = deviceId;
            touch();
        }

        private void touch() {
            this.lastSeenEpochMs = System.currentTimeMillis();
        }

        private boolean isOnline() {
            return (System.currentTimeMillis() - lastSeenEpochMs) <= OFFLINE_TIMEOUT_MS;
        }
    }

    private static final class DeviceState {
        private static final int VELOCITY_WINDOW_SIZE = 20;
        private static final double SMOOTHING_ALPHA = 0.25;
        private static final int CALIBRATION_WINDOWS = 8;
        private static final double DETECT_SCORE_THRESHOLD = 0.32;
        private static final double MEDIUM_SCORE_THRESHOLD = 0.20;
        private static final double CLEAR_SCORE_THRESHOLD = 0.12;
        private static final int REQUIRED_STREAK = 3;
        private static final long FLAG_HOLD_MS = 10_000L;
        private static final int NO_SAMPLE_RESET_TICKS = 8;
        private static final int INACTIVE_GAME_RESET_TICKS = 2;
        private static final double MIN_SEGMENT_DT_SEC = 0.003;
        private static final double MAX_REASONABLE_SPEED = 12_000.0;
        private static final int DESYNC_STREAK_THRESHOLD = 3;
        private static final double EVIDENCE_FLAG_THRESHOLD = 0.78;
        private static final double EVIDENCE_CLEAR_THRESHOLD = 0.22;
        private static final int CLEAR_WINDOWS_REQUIRED = 4;
        private static final int MIN_SAMPLES_FOR_SCORING = 6;
        private static final long GAME_WARMUP_MS = 6_000L;

        private final String deviceId;
        private final Deque<Double> velocityWindow = new ArrayDeque<>();
        private Map<String, Object> latestPayload = Collections.emptyMap();

        private double lastVelocity = 0;
        private double velocityAverage = 0;
        private double smoothedVelocity = 0;
        private double cheatScore = 0;
        private boolean isCheater = false;
        private int suspiciousStreak = 0;
        private int cleanStreak = 0;
        private int lastSamples = 0;
        private double lastStraightness = 0;
        private double lastDirectionChangeRate = 0;
        private double lastMaxSpeed = 0;
        private double lastClickRate = 0;
        private double lastSnapClickRate = 0;
        private double lastScreenMotion = 0;
        private double threatLevel = 0;
        private String threatReason = "none";
        private String riskLevel = "clear";
        private long flagHoldUntilEpochMs = 0L;
        private int noSampleTicks = 0;
        private int inactiveGameTicks = 0;
        private int screenInputDesyncStreak = 0;
        private int modEvidenceStreak = 0;
        private double evidenceScore = 0;
        private boolean lastGameActive = false;
        private long gameActiveSinceEpochMs = 0L;
        private boolean sessionFlagRecorded = false;
        private int detectionCount = 0;
        private long firstDetectedAt = 0L;
        private long lastDetectedAt = 0L;

        private DeviceState(String deviceId) {
            this.deviceId = deviceId;
        }

        @SuppressWarnings("unchecked")
        private synchronized void update(Map<String, Object> payload) {
            this.latestPayload = payload != null ? payload : Collections.emptyMap();
            try {
                List<Map<String, Object>> mouse = (List<Map<String, Object>>) latestPayload.get("mouse");
                MovementFeatures features = extractFeatures(mouse);
                lastSamples = features.samples;
                lastStraightness = features.straightness;
                lastDirectionChangeRate = features.directionChangeRate;
                lastMaxSpeed = features.maxSpeed;
                lastClickRate = features.clickRate;
                lastSnapClickRate = features.snapClickRate;
                Map<String, Object> screen = asMap(latestPayload.get("screen"));
                lastScreenMotion = readNumber(screen, "motion", 0);
                Map<String, Object> game = asMap(latestPayload.get("game"));
                boolean gameActive = Boolean.TRUE.equals(game.get("active"));

                lastVelocity = features.currentVelocity;
                updateVelocityHistory(lastVelocity);
                velocityAverage = computeWindowAverage();
                smoothedVelocity = (SMOOTHING_ALPHA * lastVelocity) + ((1 - SMOOTHING_ALPHA) * smoothedVelocity);

                cheatScore = computeCheatScore(features, lastScreenMotion, gameActive);
                ThreatSignal signal = evaluateThreatSignal(features, cheatScore, lastScreenMotion, gameActive);
                threatReason = signal.reason;
                updateCheatState(signal, gameActive);
                persistSnapshot(buildLiveState());
            } catch (Exception ignored) {
                lastVelocity = 0;
                updateVelocityHistory(0);
                velocityAverage = computeWindowAverage();
                smoothedVelocity = (SMOOTHING_ALPHA * 0) + ((1 - SMOOTHING_ALPHA) * smoothedVelocity);
                cheatScore = 0;
                isCheater = false;
                suspiciousStreak = 0;
                cleanStreak = 0;
                lastSamples = 0;
                lastStraightness = 0;
                lastDirectionChangeRate = 0;
                lastMaxSpeed = 0;
                lastClickRate = 0;
                lastSnapClickRate = 0;
                lastScreenMotion = 0;
                threatLevel = 0;
                threatReason = "none";
                riskLevel = "clear";
                flagHoldUntilEpochMs = 0L;
                noSampleTicks = 0;
                inactiveGameTicks = 0;
                screenInputDesyncStreak = 0;
                modEvidenceStreak = 0;
                evidenceScore = 0;
                lastGameActive = false;
                gameActiveSinceEpochMs = 0L;
                sessionFlagRecorded = false;
                persistSnapshot(buildLiveState());
            }
        }

        private synchronized Map<String, Object> getLiveState() {
            Map<String, Object> out = new HashMap<>(buildLiveState());
            AgentPresence presence = AGENT_PRESENCE.get(deviceId);
            out.put("online", presence != null && presence.isOnline());
            return out;
        }

        private synchronized List<Map<String, Object>> getSnapshots(int limit) {
            int safeLimit = Math.max(1, limit);
            Path snapshotPath = resolveSnapshotPath(deviceId);
            if (!Files.exists(snapshotPath)) {
                return Collections.emptyList();
            }

            try {
                List<String> lines = Files.readAllLines(snapshotPath, StandardCharsets.UTF_8);
                int start = Math.max(0, lines.size() - safeLimit);
                List<Map<String, Object>> snapshots = new ArrayList<>();
                for (int i = start; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (!line.isEmpty()) {
                        snapshots.add(MAPPER.readValue(line, MAP_TYPE));
                    }
                }
                return snapshots;
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        private Map<String, Object> buildLiveState() {
            Object mouse = latestPayload.getOrDefault("mouse", Collections.emptyList());
            Object keyboard = latestPayload.getOrDefault("keys", Collections.emptyList());
            Map<String, Object> game = asMap(latestPayload.get("game"));
            Map<String, Object> out = new HashMap<>();
            out.put("timestamp", Instant.now().toEpochMilli());
            out.put("deviceId", deviceId);
            out.put("mouse", mouse);
            out.put("velocity", lastVelocity);
            out.put("velocityAvg", velocityAverage);
            out.put("velocitySmoothed", smoothedVelocity);
            out.put("keyboard", keyboard);
            out.put("cheatScore", cheatScore);
            out.put("suspiciousStreak", suspiciousStreak);
            out.put("isCheater", isCheater);
            out.put("sampleCount", lastSamples);
            out.put("straightness", lastStraightness);
            out.put("directionChangeRate", lastDirectionChangeRate);
            out.put("maxSpeed", lastMaxSpeed);
            out.put("clickRate", lastClickRate);
            out.put("snapClickRate", lastSnapClickRate);
            out.put("screenMotion", lastScreenMotion);
            out.put("threatLevel", threatLevel);
            out.put("threatReason", threatReason);
            out.put("riskLevel", riskLevel);
            out.put("evidenceScore", evidenceScore);
            out.put("modEvidenceStreak", modEvidenceStreak);
            out.put("gameActive", Boolean.TRUE.equals(game.get("active")));
            out.put("gameExe", safeString(game.get("exe"), "unknown"));
            out.put("detectionCount", detectionCount);
            out.put("firstDetectedAt", firstDetectedAt);
            out.put("lastDetectedAt", lastDetectedAt);
            return out;
        }

        private void persistSnapshot(Map<String, Object> snapshot) {
            Path snapshotPath = resolveSnapshotPath(deviceId);
            try {
                Path parent = snapshotPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                String jsonLine = MAPPER.writeValueAsString(snapshot) + System.lineSeparator();
                synchronized (SNAPSHOT_FILE_LOCK) {
                    Files.writeString(snapshotPath, jsonLine, StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }
            } catch (IOException ignored) {
                // Non-fatal.
            }
        }

        private void updateVelocityHistory(double value) {
            velocityWindow.addLast(value);
            if (velocityWindow.size() > VELOCITY_WINDOW_SIZE) {
                velocityWindow.removeFirst();
            }
        }

        private double computeWindowAverage() {
            if (velocityWindow.isEmpty()) {
                return 0;
            }
            double sum = 0;
            for (double v : velocityWindow) {
                sum += v;
            }
            return sum / velocityWindow.size();
        }

        private double computeCheatScore(MovementFeatures f, double screenMotion, boolean gameActive) {
            if (!gameActive) {
                return 0;
            }
            if (velocityWindow.size() < CALIBRATION_WINDOWS || f.samples < MIN_SAMPLES_FOR_SCORING) {
                return 0;
            }

            double baseline = Math.max(120, Math.max(velocityAverage, smoothedVelocity));
            double burstRatio = f.maxSpeed / baseline;
            double accelRatio = f.maxAcceleration / (baseline * 35.0);
            double jerkRatio = f.maxJerk / (baseline * 900.0);
            double clickRatio = f.clickRate / 18.0;
            double triggerSignal = clamp01((f.clickRate - 4.5) / 8.0);
            double snapTriggerSignal = clamp01(f.snapClickRate);
            double screenSignal = clamp01((screenMotion - 8.0) / 18.0);
            double hyperSpeedSignal = clamp01((f.maxSpeed - 1200.0) / 3500.0);
            double aimLockSignal =
                    clamp01((f.straightness - 0.972) / 0.02)
                            * clamp01((0.09 - f.directionChangeRate) / 0.09)
                            * clamp01((f.maxSpeed - 850.0) / 1600.0);
            double lowMotionSnapSignal =
                    clamp01((f.maxSpeed - 1400.0) / 2600.0)
                            * clamp01((7.0 - screenMotion) / 7.0)
                            * clamp01((0.18 - f.directionChangeRate) / 0.18);

            double patternConfidence = 0;
            if (f.straightness > 0.988 && f.directionChangeRate < 0.035 && burstRatio > 3.4) {
                patternConfidence = 1.0;
            } else if (f.straightness > 0.980 && f.directionChangeRate < 0.060 && burstRatio > 2.6) {
                patternConfidence = 0.6;
            }

            double directionalConsistency = clamp01((0.35 - f.directionChangeRate) / 0.35);
            double speedReliability = 0.25 + (0.75 * directionalConsistency);
            double score =
                    (0.16 * clamp01((burstRatio - 2.2) / 2.0) * speedReliability) +
                    (0.12 * clamp01((accelRatio - 1.2) / 2.0) * speedReliability) +
                    (0.08 * clamp01((jerkRatio - 1.1) / 2.2) * speedReliability) +
                    (0.12 * patternConfidence) +
                    (0.08 * clamp01((f.straightness - 0.96) / 0.04)) +
                    (0.05 * clamp01((0.22 - f.directionChangeRate) / 0.22)) +
                    (0.02 * clamp01(clickRatio)) +
                    (0.03 * triggerSignal) +
                    (0.08 * snapTriggerSignal) +
                    (0.20 * screenSignal) +
                    (0.14 * hyperSpeedSignal) +
                    (0.14 * aimLockSignal) +
                    (0.08 * lowMotionSnapSignal);

            double sampleConfidence = clamp01((f.samples - 4) / 8.0);
            score *= sampleConfidence;
            return clamp01(score);
        }

        private void updateCheatState(ThreatSignal signal, boolean gameActive) {
            long now = System.currentTimeMillis();
            boolean wasCheater = isCheater;
            boolean hasSamples = lastSamples > 0 && gameActive;
            boolean enoughSamples = lastSamples >= MIN_SAMPLES_FOR_SCORING;
            if (gameActive && !lastGameActive) {
                gameActiveSinceEpochMs = now;
                sessionFlagRecorded = false;
                flagHoldUntilEpochMs = 0L;
                isCheater = false;
                suspiciousStreak = 0;
                cleanStreak = 0;
                modEvidenceStreak = 0;
                evidenceScore = Math.max(0, evidenceScore * 0.2);
                threatLevel = Math.max(0, threatLevel * 0.3);
            }
            boolean inWarmup = gameActive && (now - gameActiveSinceEpochMs) < GAME_WARMUP_MS;
            double effectiveCheatScore = inWarmup && !signal.instantFlag ? 0.0 : cheatScore;

            if (!gameActive) {
                inactiveGameTicks++;
            } else {
                inactiveGameTicks = 0;
            }

            if (hasSamples) {
                noSampleTicks = 0;
                double effectiveSignal = inWarmup && !signal.instantFlag ? 0.0 : signal.score;
                threatLevel = clamp01((threatLevel * 0.78) + (effectiveCheatScore * 0.42) + (effectiveSignal * 0.50));
            } else {
                noSampleTicks++;
                threatLevel = Math.max(0, threatLevel - 0.06);
                suspiciousStreak = Math.max(0, suspiciousStreak - 2);
            }

            if (signal.instantFlag) {
                suspiciousStreak = Math.max(REQUIRED_STREAK + 1, suspiciousStreak + 2);
                cleanStreak = 0;
                flagHoldUntilEpochMs = Math.max(flagHoldUntilEpochMs, now + FLAG_HOLD_MS);
            } else if (!inWarmup && hasSamples && (effectiveCheatScore >= DETECT_SCORE_THRESHOLD || threatLevel >= 0.55)) {
                suspiciousStreak++;
                cleanStreak = 0;
                flagHoldUntilEpochMs = Math.max(flagHoldUntilEpochMs, now + 3_000L);
            } else if (!hasSamples && screenInputDesyncStreak >= DESYNC_STREAK_THRESHOLD) {
                suspiciousStreak++;
                cleanStreak = 0;
            } else if (hasSamples
                    && !inWarmup
                    && effectiveCheatScore >= MEDIUM_SCORE_THRESHOLD
                    && lastSamples >= 12
                    && (lastMaxSpeed > 900 || lastStraightness > 0.60 || lastSnapClickRate > 0.35 || lastScreenMotion > 16.0)) {
                suspiciousStreak++;
                cleanStreak = Math.max(0, cleanStreak - 1);
            } else if (effectiveCheatScore <= CLEAR_SCORE_THRESHOLD || !hasSamples) {
                cleanStreak++;
                suspiciousStreak = Math.max(0, suspiciousStreak - 1);
            } else {
                cleanStreak = Math.max(0, cleanStreak - 1);
            }

            suspiciousStreak = Math.min(12, Math.max(0, suspiciousStreak));

            boolean hasModEvidence =
                    signal.instantFlag
                            || "aim-lock".equals(threatReason)
                            || "snap-trigger".equals(threatReason)
                            || "rage-pattern".equals(threatReason)
                            || "impossible-speed".equals(threatReason)
                            || "wall-track".equals(threatReason)
                            || "input-desync".equals(threatReason);
            if (!inWarmup && hasModEvidence && hasSamples) {
                modEvidenceStreak = Math.min(8, modEvidenceStreak + 1);
            } else {
                modEvidenceStreak = Math.max(0, modEvidenceStreak - 1);
            }

            double evidenceContribution = 0;
            if (signal.instantFlag) {
                evidenceContribution = 0.45;
            } else if (!inWarmup && hasSamples && enoughSamples) {
                evidenceContribution = clamp01((effectiveCheatScore * 0.62) + (signal.score * 0.38));
            } else if (!hasSamples && screenInputDesyncStreak >= DESYNC_STREAK_THRESHOLD) {
                evidenceContribution = 0.18;
            }

            if (evidenceContribution > 0) {
                evidenceScore = clamp01((evidenceScore * 0.82) + evidenceContribution);
            } else {
                double decay = gameActive ? 0.04 : 0.09;
                evidenceScore = Math.max(0, evidenceScore - decay);
            }

            if (!gameActive && inactiveGameTicks >= INACTIVE_GAME_RESET_TICKS) {
                isCheater = false;
                suspiciousStreak = 0;
                cleanStreak = 0;
                threatLevel = Math.max(0, threatLevel - 0.20);
                evidenceScore = Math.max(0, evidenceScore - 0.30);
                threatReason = "low";
                riskLevel = evidenceScore >= 0.32 ? "suspect" : "clear";
                lastGameActive = gameActive;
                return;
            }

            if (!hasSamples && noSampleTicks >= NO_SAMPLE_RESET_TICKS) {
                isCheater = false;
                suspiciousStreak = 0;
                cleanStreak = 0;
                threatLevel = Math.max(0, threatLevel - 0.15);
                evidenceScore = Math.max(0, evidenceScore - 0.20);
                riskLevel = evidenceScore >= 0.32 ? "suspect" : "clear";
                lastGameActive = gameActive;
                return;
            }

            boolean inHold = now < flagHoldUntilEpochMs;
            boolean sustainedModSignal =
                    !inWarmup
                            && modEvidenceStreak >= 2
                            && (evidenceScore >= EVIDENCE_FLAG_THRESHOLD || suspiciousStreak >= REQUIRED_STREAK || threatLevel >= 0.78);
            if (signal.instantFlag || sustainedModSignal || inHold) {
                isCheater = true;
            } else if ((cleanStreak >= CLEAR_WINDOWS_REQUIRED && evidenceScore <= EVIDENCE_CLEAR_THRESHOLD) || noSampleTicks >= NO_SAMPLE_RESET_TICKS) {
                isCheater = false;
            }

            if (isCheater && (signal.instantFlag || threatLevel >= 0.80 || evidenceScore >= 0.88 || "impossible-speed".equals(threatReason))) {
                riskLevel = "confirmed";
            } else if (isCheater || threatLevel >= 0.50 || evidenceScore >= 0.55 || suspiciousStreak >= 1) {
                riskLevel = "high-risk";
            } else if (threatLevel >= 0.20 || evidenceScore >= 0.32 || cheatScore >= MEDIUM_SCORE_THRESHOLD) {
                riskLevel = "suspect";
            } else {
                riskLevel = "clear";
            }

            if (!wasCheater && isCheater) {
                if (!sessionFlagRecorded) {
                    detectionCount += 1;
                    sessionFlagRecorded = true;
                    if (firstDetectedAt <= 0L) {
                        firstDetectedAt = now;
                    }
                }
                lastDetectedAt = now;
            } else if (isCheater && lastDetectedAt > 0L) {
                lastDetectedAt = now;
            }
            lastGameActive = gameActive;
        }

        private ThreatSignal evaluateThreatSignal(MovementFeatures f, double score, double screenMotion, boolean gameActive) {
            double signalScore = 0;
            String reason = "low";
            boolean instantFlag = false;

            boolean strongSnapTrigger = f.snapClickRate >= 0.30 && f.maxSpeed > 850 && screenMotion > 8;
            boolean highAimLock = f.straightness > 0.975 && f.directionChangeRate < 0.08 && f.maxSpeed > 900;
            boolean wallTrackPattern =
                    gameActive
                            && f.samples >= 8
                            && f.straightness > 0.992
                            && f.directionChangeRate < 0.03
                            && f.maxSpeed > 1200
                            && screenMotion > 1.0
                            && screenMotion < 16.0;
            boolean ragePattern = score > 0.72 && f.clickRate > 8.0 && screenMotion > 16;
            boolean impossibleSpeedPattern =
                    f.maxSpeed > 8_500
                            && f.samples >= 8
                            && (screenMotion > 1.0 || f.clickRate > 2.0 || f.snapClickRate > 0.12);
            boolean jumpTrackingPattern = f.maxSpeed > 2_500 && screenMotion > 18 && f.samples >= 10;
            boolean screenInputDesyncPattern = gameActive && f.samples <= 2 && screenMotion > 22.0;

            if (screenInputDesyncPattern) {
                screenInputDesyncStreak++;
            } else {
                screenInputDesyncStreak = Math.max(0, screenInputDesyncStreak - 1);
            }

            if (strongSnapTrigger) {
                signalScore += 0.95;
                reason = "snap-trigger";
                instantFlag = true;
            }
            if (highAimLock) {
                signalScore = Math.max(signalScore, 0.88);
                reason = "aim-lock";
                instantFlag = true;
            }
            if (wallTrackPattern) {
                signalScore = Math.max(signalScore, 0.84);
                reason = "wall-track";
                instantFlag = true;
            }
            if (ragePattern) {
                signalScore = Math.max(signalScore, 0.82);
                reason = "rage-pattern";
            }
            if (jumpTrackingPattern) {
                signalScore = Math.max(signalScore, 0.76);
                reason = "jump-tracking";
            }
            if (impossibleSpeedPattern) {
                signalScore = Math.max(signalScore, 0.92);
                reason = "impossible-speed";
                instantFlag = true;
            }
            if (screenInputDesyncStreak >= DESYNC_STREAK_THRESHOLD) {
                signalScore = Math.max(signalScore, 0.68);
                reason = "input-desync";
            }
            if (signalScore == 0 && score >= 0.56 && screenMotion > 14) {
                signalScore = 0.62;
                reason = "elevated-motion";
            }
            return new ThreatSignal(clamp01(signalScore), reason, instantFlag);
        }

        @SuppressWarnings("unchecked")
        private MovementFeatures extractFeatures(List<Map<String, Object>> mouse) {
            if (mouse == null || mouse.size() < 2) {
                return MovementFeatures.empty();
            }

            double pathDistance = 0;
            double duration = 0;
            double speedSum = 0;
            double maxSpeed = 0;
            double maxAcceleration = 0;
            double maxJerk = 0;
            double directionChangeSum = 0;
            int speedSamples = 0;
            int directionSamples = 0;
            int clickCount = 0;
            int snapEventCount = 0;
            int snapClickCount = 0;

            double firstX = readCoordinate(mouse.get(0), "cx", "x", 0);
            double firstY = readCoordinate(mouse.get(0), "cy", "y", 0);
            double lastX = firstX;
            double lastY = firstY;
            double prevT = readNumber(mouse.get(0), "t", 0);
            double lastSnapTime = -1;

            Double prevSpeed = null;
            Double prevAcceleration = null;
            Double prevDirection = null;

            for (int i = 1; i < mouse.size(); i++) {
                Map<String, Object> p = mouse.get(i);
                double x = readCoordinate(p, "cx", "x", lastX);
                double y = readCoordinate(p, "cy", "y", lastY);
                double t = readNumber(p, "t", prevT);

                boolean clickPressed = Boolean.TRUE.equals(p.get("pressed"));
                if (clickPressed) {
                    clickCount++;
                }

                double dx = x - lastX;
                double dy = y - lastY;
                double dist = Math.hypot(dx, dy);
                double dt = t - prevT;
                if (dt < MIN_SEGMENT_DT_SEC) {
                    lastX = x;
                    lastY = y;
                    prevT = t;
                    continue;
                }
                if (dist < 1.2 && !clickPressed) {
                    duration += dt;
                    lastX = x;
                    lastY = y;
                    prevT = t;
                    continue;
                }

                double speed = dist / dt;
                if (speed > MAX_REASONABLE_SPEED) {
                    lastX = x;
                    lastY = y;
                    prevT = t;
                    continue;
                }

                pathDistance += dist;
                duration += dt;
                speedSum += speed;
                speedSamples++;
                maxSpeed = Math.max(maxSpeed, speed);
                if (speed > 1400 && dist > 6) {
                    snapEventCount++;
                    lastSnapTime = t;
                }
                if (clickPressed && lastSnapTime > 0 && (t - lastSnapTime) <= 0.12) {
                    snapClickCount++;
                }

                double direction = Math.atan2(dy, dx);
                if (prevDirection != null) {
                    directionChangeSum += Math.abs(normalizeAngle(direction - prevDirection));
                    directionSamples++;
                }
                prevDirection = direction;

                if (prevSpeed != null) {
                    double acceleration = Math.abs((speed - prevSpeed) / dt);
                    maxAcceleration = Math.max(maxAcceleration, acceleration);
                    if (prevAcceleration != null) {
                        double jerk = Math.abs((acceleration - prevAcceleration) / dt);
                        maxJerk = Math.max(maxJerk, jerk);
                    }
                    prevAcceleration = acceleration;
                }
                prevSpeed = speed;

                lastX = x;
                lastY = y;
                prevT = t;
            }

            double netDisplacement = Math.hypot(lastX - firstX, lastY - firstY);
            double straightness = pathDistance > 0 ? netDisplacement / pathDistance : 0;
            double currentVelocity = duration > 0 ? pathDistance / duration : 0;
            double meanSpeed = speedSamples > 0 ? speedSum / speedSamples : 0;
            double directionChangeRate = directionSamples > 0 ? directionChangeSum / directionSamples : 0;
            double clickRate = duration > 0 ? clickCount / duration : 0;
            double snapClickRate = snapEventCount > 0 ? ((double) snapClickCount / snapEventCount) : 0;

            return new MovementFeatures(
                    mouse.size(),
                    currentVelocity,
                    meanSpeed,
                    maxSpeed,
                    maxAcceleration,
                    maxJerk,
                    straightness,
                    directionChangeRate,
                    clickRate,
                    snapClickRate
            );
        }

        private double readCoordinate(
                Map<String, Object> source,
                String primaryKey,
                String fallbackKey,
                double fallback) {
            double primary = readNumber(source, primaryKey, Double.NaN);
            if (!Double.isNaN(primary)) {
                return primary;
            }
            return readNumber(source, fallbackKey, fallback);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> asMap(Object value) {
            if (value instanceof Map<?, ?> raw) {
                return (Map<String, Object>) raw;
            }
            return Collections.emptyMap();
        }

        private double readNumber(Map<String, Object> source, String key, double fallback) {
            if (source == null) {
                return fallback;
            }
            Object val = source.get(key);
            if (val instanceof Number n) {
                return n.doubleValue();
            }
            return fallback;
        }
    }

    private static double normalizeAngle(double angle) {
        double a = angle;
        while (a > Math.PI) {
            a -= 2 * Math.PI;
        }
        while (a < -Math.PI) {
            a += 2 * Math.PI;
        }
        return a;
    }

    private static double clamp01(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private record MovementFeatures(
            int samples,
            double currentVelocity,
            double meanSpeed,
            double maxSpeed,
            double maxAcceleration,
            double maxJerk,
            double straightness,
            double directionChangeRate,
            double clickRate,
            double snapClickRate
    ) {
        static MovementFeatures empty() {
            return new MovementFeatures(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private record ThreatSignal(
            double score,
            String reason,
            boolean instantFlag
    ) {
    }
}
