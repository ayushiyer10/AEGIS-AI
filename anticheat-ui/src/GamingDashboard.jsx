import React, { useMemo, useRef, useState, useEffect } from "react";
import axios from "axios";

import {
  Activity,
  ShieldAlert,
  Users,
  Target,
  Bell,
  RefreshCw
} from "lucide-react";

import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";

import "./GamingDashboard.css";

const ENGINE_URL = import.meta.env.VITE_ENGINE_URL || "";

export default function GamingDashboard({ isActive = true }) {
  const [data, setData] = useState([{ time: "00:00", velocity: 0, velocitySmoothed: 0 }]);
  const [alerts, setAlerts] = useState([]);
  const [flaggedPlayerTimeline, setFlaggedPlayerTimeline] = useState({});
  const [isConnected, setIsConnected] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [devices, setDevices] = useState([]);
  const [selectedDeviceId, setSelectedDeviceId] = useState("");
  const [activeDeviceId, setActiveDeviceId] = useState("unknown");
  const [latestVelocity, setLatestVelocity] = useState(0);
  const [latestVelocitySmoothed, setLatestVelocitySmoothed] = useState(0);
  const [activeCheaterCount, setActiveCheaterCount] = useState(0);
  const lastSeenFlagSignatureRef = useRef({});
  const failCountRef = useRef(0);
  const lastErrorLogRef = useRef(0);
  const isDesktopLite =
    typeof window !== "undefined" &&
    new URLSearchParams(window.location.search).get("desktop") === "1";
  const flaggedPlayerRows = useMemo(() => {
    return Object.values(flaggedPlayerTimeline)
      .sort((a, b) => (b.lastDetectedAt || 0) - (a.lastDetectedAt || 0));
  }, [flaggedPlayerTimeline]);

  const formatDetectionTime = (epochMs) => {
    if (!epochMs) return "--";
    return new Date(epochMs).toLocaleString();
  };

  /* =========================
     LIVE DATA POLLING
  ========================= */
  const fetchData = async () => {
    try {
      const devicesRes = await axios.get(`${ENGINE_URL}/api/devices`);
      const currentDevices = Array.isArray(devicesRes.data) ? devicesRes.data : [];
      setDevices(currentDevices);
      const currentlyFlagged = currentDevices.filter(d => d.isCheater === true);
      setActiveCheaterCount(currentlyFlagged.length);
      setFlaggedPlayerTimeline(prev => {
        const next = { ...prev };
        const now = Date.now();
        currentDevices.forEach(device => {
          const detections = Number(device?.detectionCount || 0);
          const isCheaterNow = device?.isCheater === true;
          if (detections <= 0 && !isCheaterNow) return;
          const deviceId = device?.deviceId || "unknown";
          const existing = next[deviceId];
          const firstDetectedAt = Number(device?.firstDetectedAt || existing?.firstDetectedAt || (isCheaterNow ? now : 0));
          const lastDetectedAt = Number(device?.lastDetectedAt || existing?.lastDetectedAt || (isCheaterNow ? now : 0));
          next[deviceId] = {
            deviceId,
            firstDetectedAt,
            lastDetectedAt,
            flagCount: Math.max(detections, Number(existing?.flagCount || 0), isCheaterNow ? 1 : 0),
            lastConfidence: Math.round(Math.min(100, Math.max(0, Number(device?.cheatScore || 0) * 100)))
          };
        });
        return next;
      });
      const newAlerts = [];
      currentDevices.forEach(device => {
        const deviceId = device?.deviceId || "unknown";
        const detections = Number(device?.detectionCount || 0);
        const lastDetectedAt = Number(device?.lastDetectedAt || 0);
        if (detections <= 0 || lastDetectedAt <= 0) return;
        const signature = `${detections}:${lastDetectedAt}`;
        const previousSignature = lastSeenFlagSignatureRef.current[deviceId];
        if (previousSignature !== signature) {
          newAlerts.push({
            id: `${deviceId}-${lastDetectedAt}`,
            deviceId,
            confidence: Math.round(Math.min(100, Math.max(0, Number(device?.threatLevel || device?.cheatScore || 0) * 100))),
            timestamp: lastDetectedAt
          });
          lastSeenFlagSignatureRef.current[deviceId] = signature;
        }
      });
      if (newAlerts.length > 0) {
        setAlerts(prev => [...newAlerts.sort((a, b) => b.timestamp - a.timestamp), ...prev].slice(0, 30));
      }

      const nextDeviceId = selectedDeviceId || currentDevices[0]?.deviceId || "";
      if (!selectedDeviceId && nextDeviceId) {
        setSelectedDeviceId(nextDeviceId);
      }

      const liveUrl = nextDeviceId
        ? `${ENGINE_URL}/api/devices/${encodeURIComponent(nextDeviceId)}/live`
        : `${ENGINE_URL}/api/live`;
      const res = await axios.get(liveUrl);
      if (!res.data) return;
      setIsConnected(true);

      const velocity = Math.min(100, Math.abs(res.data.velocity || 0) * 0.03);
      const velocitySmoothed = Math.min(100, Math.abs(res.data.velocitySmoothed || 0) * 0.03);
      setLatestVelocity(Math.round(velocity));
      setLatestVelocitySmoothed(Math.round(velocitySmoothed));
      const activeId = nextDeviceId || res.data.deviceId || "unknown";
      setActiveDeviceId(activeId);

      const time = new Date().toLocaleTimeString([], {
        minute: "2-digit",
        second: "2-digit"
      });

      setData(prev => [...prev.slice(isDesktopLite ? -12 : -20), { time, velocity, velocitySmoothed }]);
      failCountRef.current = 0; // Reset on success
    } catch {
      failCountRef.current++;
      setIsConnected(false);
      // Only log once every 30 seconds to reduce spam
      const now = Date.now();
      if (now - lastErrorLogRef.current > 30000) {
        console.warn("Engine offline - start backend with: cd engine && mvn spring-boot:run");
        lastErrorLogRef.current = now;
      }
    }
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await fetchData();
    setTimeout(() => setIsRefreshing(false), 1000);
  };

  useEffect(() => {
    if (!isActive) return undefined;
    const basePoll = isDesktopLite ? 6500 : 2800;
    // Slow down polling when backend is offline (max 15s)
    const getPollInterval = () => Math.min(15000, basePoll * Math.pow(1.5, Math.min(failCountRef.current, 4)));

    let timeoutId;
    const poll = async () => {
      await fetchData();
      timeoutId = setTimeout(poll, getPollInterval());
    };
    poll();
    return () => clearTimeout(timeoutId);
  }, [selectedDeviceId, isDesktopLite, isActive]);

  return (
    <>
      <header className="header">
        <span className="header-label">SECURITY TERMINAL V1.0</span>
        <div className="header-right">
          <select
            className="device-select"
            value={selectedDeviceId}
            onChange={e => {
              setSelectedDeviceId(e.target.value);
              setData([]);
            }}
          >
            {devices.length === 0 && <option value="">No devices</option>}
            {devices.map(d => (
              <option key={d.deviceId} value={d.deviceId}>
                {d.hostname || d.deviceId}
              </option>
            ))}
          </select>
          <span className="header-label">DEVICE: {activeDeviceId}</span>
          <RefreshCw 
            size={18} 
            className={isRefreshing ? 'spinning' : ''}
            onClick={handleRefresh}
          />
          <Bell size={18} />
          <span className={`status ${isConnected ? 'connected' : 'disconnected'}`}>
            {isConnected ? 'SYSTEM ONLINE' : 'ENGINE OFFLINE'}
          </span>
        </div>
      </header>

      {/* STATS */}
      <section className="stats-grid">
          {[
            { label: "Active Agents", value: devices.filter(d => d.online).length, icon: <Users size={18} />, tooltip: "Online monitored devices" },
            {
              label: "Cheaters Flagged",
              value: activeCheaterCount,
              icon: <ShieldAlert size={18} />,
              tooltip: "Currently flagged players"
            },
            { label: "Velocity", value: latestVelocity, icon: <Activity size={18} />, tooltip: "Current player movement velocity" },
            { label: "Smoothed Velocity", value: latestVelocitySmoothed, icon: <Target size={18} />, tooltip: "Rolling average movement velocity" }
          ].map((item, i) => (
            <div
              key={item.label}
              className="stat-box"
              title={item.tooltip}
            >
              <div className="stat-top">
                <span>{item.label}</span>
                {item.icon}
              </div>
              <div className="stat-value">{item.value}</div>
            </div>
          ))}
        </section>

        {/* ANALYTICS */}
        <section className="analytics">
          <div className="panel">
            <h3>BEHAVIORAL VELOCITY</h3>
            <div className="chart-wrapper">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={data}>
                  <XAxis dataKey="time" hide />
                  <YAxis hide domain={[0, 100]} />
                  <Tooltip
                    cursor={false}
                    contentStyle={{
                      background: "#0a0a0a",
                      border: "1px solid #1f2937",
                      borderRadius: "8px",
                      padding: "8px 12px",
                      color: "#fff"
                    }}
                    labelStyle={{ color: "#9ca3af", fontSize: "12px" }}
                    itemStyle={{ color: "#ef4444", fontSize: "14px", fontWeight: "bold" }}
                  />
                  <Area
                    type="monotone"
                    dataKey="velocity"
                    stroke="#ef4444"
                    strokeWidth={3}
                    fill="rgba(239,68,68,0.35)"
                    activeDot={isDesktopLite ? false : { r: 8, fill: '#ef4444', stroke: 'rgba(239,68,68,0.4)', strokeWidth: 6, style: { filter: 'drop-shadow(0 0 8px rgba(239,68,68,0.7))', transition: 'all 0.3s ease' } }}
                  />
                  <Area
                    type="monotone"
                    dataKey="velocitySmoothed"
                    stroke="#22c55e"
                    strokeWidth={2}
                    fill="rgba(34,197,94,0.12)"
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="panel">
            <h3>SECURITY LOGS</h3>
            <div className="alerts">
              {alerts.length === 0 ? (
                <div className="empty-state">
                  <ShieldAlert size={32} opacity={0.3} />
                  <p>No threats detected</p>
                </div>
              ) : (
                alerts.map(alert => (
                  <div key={alert.id} className="alert-item">
                    <div className="alert-main">
                      <strong>{alert.deviceId}</strong>
                      <small>{formatDetectionTime(alert.timestamp)}</small>
                    </div>
                    <span>{alert.confidence.toFixed(1)}%</span>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="panel flagged-panel">
            <h3>FLAGGED PLAYERS</h3>
            <div className="flagged-list">
              {flaggedPlayerRows.length === 0 ? (
                <div className="empty-state">
                  <ShieldAlert size={32} opacity={0.3} />
                  <p>No flagged players yet</p>
                </div>
              ) : (
                flaggedPlayerRows.map(player => (
                  <div key={player.deviceId} className="flagged-item">
                    <div className="flagged-id">{player.deviceId}</div>
                    <div className="flagged-meta">
                      <span>First: {formatDetectionTime(player.firstDetectedAt)}</span>
                      <span>Last: {formatDetectionTime(player.lastDetectedAt)}</span>
                      <span>Flags: {player.flagCount}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </section>
      </>
  );
}
