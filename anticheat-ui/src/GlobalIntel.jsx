import React, { useEffect, useState, useRef } from "react";
import axios from "axios";
import {
  ShieldAlert,
  TrendingUp,
  AlertTriangle,
  CheckCircle,
  XCircle,
  MapPin,
  Globe,
  RefreshCw
} from "lucide-react";
import {
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import "./GlobalIntel.css";

const ENGINE_URL = import.meta.env.VITE_ENGINE_URL || "";

const getRegionFromIP = (ip, hostname) => {
  if (!ip) return "UNKNOWN";
  if (ip.startsWith("185") || ip.startsWith("91") || hostname?.includes("eu")) return "EU";
  if (ip.startsWith("203") || ip.startsWith("1.") || hostname?.includes("asia")) return "ASIA";
  if (ip.startsWith("99") || ip.startsWith("23") || hostname?.includes("na")) return "NA";
  if (ip.startsWith("101") || ip.startsWith("27") || hostname?.includes("sa")) return "SA";
  if (ip.startsWith("49") || hostname?.includes("au")) return "OCE";
  return "NA";
};

const classifyDetectionMethod = (device) => {
  const straightness = device.straightness || 0;
  const maxSpeed = device.maxSpeed || 0;
  const clickRate = device.clickRate || 0;
  const snapClickRate = device.snapClickRate || 0;
  if (snapClickRate > 0.35 && maxSpeed > 900) return "Aimbot";
  if (straightness > 0.98 && clickRate > 0.15) return "Aimbot";
  if (maxSpeed > 1200) return "Speedhack";
  if (clickRate > 0.2) return "Wallhack";
  return "Other";
};

const getDetectionPoints = (cheatScore) => {
  if (cheatScore >= 0.85) return 4;
  if (cheatScore >= 0.65) return 3;
  if (cheatScore >= 0.45) return 2;
  if (cheatScore >= 0.25) return 1;
  return 0;
};

const isDeviceBanned = (device) => {
  const cheatScore = Math.min(1, Math.max(0, device?.cheatScore || 0));
  const threatLevel = Math.min(1, Math.max(0, device?.threatLevel || 0));
  const riskLevel = (device?.riskLevel || "").toString().toLowerCase();
  return riskLevel === "confirmed" || threatLevel >= 0.75 || cheatScore >= 0.65;
};

export default function GlobalIntel({ isActive = true }) {
  const [regionData, setRegionData] = useState([
    { region: "NA", detections: 0, rate: 0 },
    { region: "EU", detections: 0, rate: 0 },
    { region: "ASIA", detections: 0, rate: 0 },
    { region: "SA", detections: 0, rate: 0 },
    { region: "OCE", detections: 0, rate: 0 }
  ]);
  const [methodData, setMethodData] = useState([
    { name: "Aimbot", value: 0, color: "#ef4444" },
    { name: "Wallhack", value: 0, color: "#f97316" },
    { name: "Speedhack", value: 0, color: "#eab308" },
    { name: "Other", value: 0, color: "#64748b" }
  ]);
  const [trendData, setTrendData] = useState([
    { week: "W1", detections: 0 },
    { week: "W2", detections: 0 },
    { week: "W3", detections: 0 },
    { week: "W4", detections: 0 },
    { week: "W5", detections: 0 },
    { week: "W6", detections: 0 }
  ]);
  const [topCheaters, setTopCheaters] = useState([]);
  const [globalStats, setGlobalStats] = useState({
    totalRegions: 0,
    totalDetections: 0,
    activeBans: 0,
    detectionRate: 0
  });
  const [liveOverwatch, setLiveOverwatch] = useState(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const failCountRef = useRef(0);
  const lastErrorLogRef = useRef(0);
  const isDesktopLite =
    typeof window !== "undefined" &&
    new URLSearchParams(window.location.search).get("desktop") === "1";

  const fetchGlobalData = async () => {
    try {
      const [devicesRes, liveRes] = await Promise.all([
        axios.get(`${ENGINE_URL}/api/devices`),
        axios.get(`${ENGINE_URL}/api/live`).catch(() => ({ data: null }))
      ]);
      const devices = Array.isArray(devicesRes.data) ? devicesRes.data : [];
      setLiveOverwatch(liveRes?.data || null);
      setIsConnected(true);

      const regionMap = {};
      const methodCountMap = { Aimbot: 0, Wallhack: 0, Speedhack: 0, Other: 0 };
      let totalDevicesCheating = 0;

      devices.forEach(device => {
        const region = getRegionFromIP(device.ip || "", device.hostname || "");
        if (!regionMap[region]) regionMap[region] = { detections: 0, online: 0 };
        if (device.online) regionMap[region].online++;

        if (device.isCheater === true) {
          const cheatScore = Math.min(1, Math.max(0, device.cheatScore || 0));
          regionMap[region].detections += getDetectionPoints(cheatScore);
          totalDevicesCheating++;
          methodCountMap[classifyDetectionMethod(device)]++;
        }
      });

      setRegionData(["NA", "EU", "ASIA", "SA", "OCE"].map(region => ({
        region,
        detections: regionMap[region]?.detections || 0,
        rate: regionMap[region]?.online ? ((regionMap[region].detections / regionMap[region].online) * 100).toFixed(1) : 0
      })));

      const totalDetectionMethods = Object.values(methodCountMap).reduce((a, b) => a + b, 0);
      const methodPercentages = [
        { name: "Aimbot", value: totalDetectionMethods ? Math.round((methodCountMap.Aimbot / totalDetectionMethods) * 100) : 0, color: "#ef4444" },
        { name: "Wallhack", value: totalDetectionMethods ? Math.round((methodCountMap.Wallhack / totalDetectionMethods) * 100) : 0, color: "#f97316" },
        { name: "Speedhack", value: totalDetectionMethods ? Math.round((methodCountMap.Speedhack / totalDetectionMethods) * 100) : 0, color: "#eab308" },
        { name: "Other", value: totalDetectionMethods ? Math.round((methodCountMap.Other / totalDetectionMethods) * 100) : 0, color: "#64748b" }
      ];
      const percentSum = methodPercentages.reduce((sum, item) => sum + item.value, 0);
      if (totalDetectionMethods > 0 && percentSum !== 100) methodPercentages[0].value += 100 - percentSum;
      setMethodData(methodPercentages);

      try {
        const snapshotsRes = await axios.get(`${ENGINE_URL}/api/live/snapshots?limit=100`);
        const snapshots = Array.isArray(snapshotsRes.data) ? snapshotsRes.data : [];
        const oneWeekMs = 7 * 24 * 60 * 60 * 1000;
        const now = Date.now();
        const counts = { W1: 0, W2: 0, W3: 0, W4: 0, W5: 0, W6: 0 };

        snapshots.forEach(snap => {
          const timestamp = snap.timestamp ? new Date(snap.timestamp).getTime() : now;
          const weeksDiff = Math.floor((now - timestamp) / oneWeekMs);
          if (weeksDiff >= 0 && weeksDiff <= 5) {
            const weekKey = `W${6 - weeksDiff}`;
            if (snap.isCheater) counts[weekKey]++;
          }
        });

        setTrendData([
          { week: "W1", detections: counts.W1 },
          { week: "W2", detections: counts.W2 },
          { week: "W3", detections: counts.W3 },
          { week: "W4", detections: counts.W4 },
          { week: "W5", detections: counts.W5 },
          { week: "W6", detections: counts.W6 }
        ]);
      } catch {
        // no-op
      }

      const historyResults = devices
        .filter(d => Number(d?.detectionCount || 0) > 0)
        .map(d => ({
          id: d.deviceId,
          region: getRegionFromIP(d.ip || "", d.hostname || ""),
          detections: Number(d.detectionCount || 0),
          lastDetectedAt: Number(d.lastDetectedAt || 0),
          current: d
        }))
        .sort((a, b) => b.detections - a.detections || b.lastDetectedAt - a.lastDetectedAt)
        .slice(0, 5);

      setTopCheaters(historyResults.map((entry, idx) => ({
        id: entry.id || `DEV_${idx + 1}`,
        region: entry.region,
        violations: entry.detections,
        lastSeen: entry.lastDetectedAt ? new Date(entry.lastDetectedAt).toLocaleDateString() : "N/A",
        banned: isDeviceBanned(entry.current)
      })));

      const activeRegions = Object.keys(regionMap).length;
      const totalDetections = historyResults.length
        ? devices.reduce((sum, d) => sum + Number(d?.detectionCount || 0), 0)
        : Object.values(regionMap).reduce((sum, r) => sum + r.detections, 0);
      const totalBanned = devices.filter(d => d.isCheater === true && isDeviceBanned(d)).length;
      const detectionRate = devices.length ? Number(((totalDevicesCheating / devices.length) * 100).toFixed(1)) : 0;

      setGlobalStats({
        totalRegions: activeRegions,
        totalDetections,
        activeBans: totalBanned,
        detectionRate
      });
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

  const handleRefresh = () => {
    setIsRefreshing(true);
    fetchGlobalData().finally(() => {
      setTimeout(() => setIsRefreshing(false), 250);
    });
  };

  useEffect(() => {
    if (!isActive) return undefined;
    const basePoll = isDesktopLite ? 7000 : 3500;
    // Slow down polling when backend is offline (max 15s)
    const getPollInterval = () => Math.min(15000, basePoll * Math.pow(1.5, Math.min(failCountRef.current, 4)));

    let timeoutId;
    const poll = async () => {
      await fetchGlobalData();
      timeoutId = setTimeout(poll, getPollInterval());
    };
    poll();
    return () => clearTimeout(timeoutId);
  }, [isDesktopLite, isActive]);

  return (
    <div className="global-intel-content">
      <header className="header">
        <span className="header-label">GLOBAL INTEL</span>
        <div className="header-right">
          <RefreshCw size={18} className={isRefreshing ? "spinning" : ""} onClick={handleRefresh} />
          <Globe size={18} className="rotating-globe" />
          <span className={`status ${isConnected ? "connected" : "disconnected"}`}>
            {isConnected ? "NETWORK ONLINE" : "ENGINE OFFLINE"}
          </span>
        </div>
      </header>

      <section className="stats-grid">
        {[
          { label: "Regions", value: globalStats.totalRegions, icon: <MapPin size={18} />, trend: "Live" },
          { label: "Detections", value: globalStats.totalDetections, icon: <ShieldAlert size={18} />, trend: "Total" },
          { label: "Active Bans", value: globalStats.activeBans, icon: <XCircle size={18} />, trend: "Enforced" },
          {
            label: "Live Threat",
            value: `${Math.round(Math.max(0, Math.min(1, Number(liveOverwatch?.threatLevel || 0))) * 100)}%`,
            icon: <TrendingUp size={18} />,
            trend: liveOverwatch?.threatReason || "realtime"
          }
        ].map(item => (
          <div key={item.label} className="stat-box">
            <div className="stat-top">
              <span>{item.label}</span>
              {item.icon}
            </div>
            <div className="stat-value">{item.value}</div>
            <div className="stat-trend">{item.trend}</div>
          </div>
        ))}
      </section>

      <section className="analytics-grid">
        <div className="panel large">
          <h3>REGIONAL DISTRIBUTION</h3>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={regionData}>
                <XAxis dataKey="region" stroke="#64748b" />
                <YAxis stroke="#64748b" />
                <Tooltip cursor={false} contentStyle={{ background: "#0b1220", border: "1px solid #1f2937", borderRadius: "8px", color: "#fff" }} />
                <Bar dataKey="detections" radius={[8, 8, 0, 0]} fill="#22d3ee" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="panel">
          <h3>DETECTION METHODS</h3>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={methodData} cx="50%" cy="50%" innerRadius={50} outerRadius={80} paddingAngle={4} dataKey="value" animationDuration={isDesktopLite ? 0 : 220}>
                  {methodData.map((entry, index) => <Cell key={`cell-${index}`} fill={entry.color} />)}
                </Pie>
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="method-legend">
            {methodData.map(item => (
              <div key={item.name} className="legend-item">
                <span className="legend-dot" style={{ background: item.color }} />
                <span>{item.name}</span>
                <span className="legend-value">{item.value}%</span>
              </div>
            ))}
          </div>
        </div>

        <div className="panel large">
          <h3>6-WEEK TREND</h3>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={trendData}>
                <XAxis dataKey="week" stroke="#64748b" />
                <YAxis stroke="#64748b" />
                <Tooltip cursor={false} contentStyle={{ background: "#0b1220", border: "1px solid #1f2937", borderRadius: "8px", color: "#fff" }} />
                <Line type="monotone" dataKey="detections" stroke="#22d3ee" strokeWidth={2.5} dot={false} activeDot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="panel">
          <h3>TOP OFFENDERS</h3>
          <div className="offenders-list">
            {topCheaters.length === 0 ? (
              <div className="offender-item">
                <div className="offender-info">
                  <div className="offender-id">No detections yet</div>
                </div>
              </div>
            ) : (
              topCheaters.map((cheater, idx) => (
                <div key={`${cheater.id}-${idx}`} className="offender-item">
                  <div className="offender-rank">#{idx + 1}</div>
                  <div className="offender-info">
                    <div className="offender-id">{cheater.id}</div>
                    <div className="offender-meta">
                      <span className="region-tag">{cheater.region}</span>
                      <span className="violation-count">{cheater.violations} detections</span>
                      <span className="violation-count">Last: {cheater.lastSeen}</span>
                    </div>
                  </div>
                  <div className={`ban-status ${cheater.banned ? "banned" : "active"}`}>
                    {cheater.banned ? <CheckCircle size={16} /> : <AlertTriangle size={16} />}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
