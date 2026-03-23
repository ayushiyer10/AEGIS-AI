import { useEffect, useState } from "react";
import "./SplashScreen.css";

const BOOT_LINES = [
  "AEGIS SECURE BOOT v3.7.1",
  "Establishing Trust Boundary...",
  "Loading Behavioral Graphs...",
  "Injecting Telemetry Hooks...",
  "Stabilizing Detection Matrix...",
  "Threat Surface: LOCKED",
  "Anti-Cheat Engine ONLINE"
];

export default function SplashScreen({ onFinish }) {
  const [lines, setLines] = useState([]);
  const [currentLine, setCurrentLine] = useState("");
  const [lineIndex, setLineIndex] = useState(0);
  const [charIndex, setCharIndex] = useState(0);
  const [progress, setProgress] = useState(0);
  const isDesktopLite =
    typeof window !== "undefined" &&
    new URLSearchParams(window.location.search).get("desktop") === "1";
  useEffect(() => {
    if (isDesktopLite) {
      let pointer = 0;
      const lineTimer = setInterval(() => {
        pointer += 1;
        setLines(BOOT_LINES.slice(0, pointer));
        setProgress(Math.round((pointer / BOOT_LINES.length) * 100));
        if (pointer >= BOOT_LINES.length) {
          clearInterval(lineTimer);
        }
      }, 180);
      return () => clearInterval(lineTimer);
    }

    if (lineIndex >= BOOT_LINES.length) return;

    const interval = setInterval(() => {
      const line = BOOT_LINES[lineIndex];

      if (charIndex < line.length) {
        setCurrentLine(prev => prev + line[charIndex]);
        setCharIndex(c => c + 1);
      } else {
        setLines(prev => [...prev, line]);
        setCurrentLine("");
        setCharIndex(0);
        setLineIndex(i => i + 1);
        setProgress(Math.round(((lineIndex + 1) / BOOT_LINES.length) * 100));
      }
    }, 12);

    return () => clearInterval(interval);
  }, [charIndex, lineIndex, isDesktopLite]);

  useEffect(() => {
    const timer = setTimeout(onFinish, isDesktopLite ? 1800 : 5000);
    return () => clearTimeout(timer);
  }, [onFinish, isDesktopLite]);

  return (
    <div className={`splash-root ${isDesktopLite ? "desktop-lite" : ""}`}>
      <div className="grid-bg" />
      <div className="noise" />
      <div className="scan-line" />

      <div className="terminal">
        {lines.map((line, i) => (
          <div key={i} className="boot-line">
            &gt; {line}
          </div>
        ))}

        {lineIndex < BOOT_LINES.length && !isDesktopLite && (
          <div className="boot-line typing">
            &gt; {currentLine}
            <span className="cursor">|</span>
          </div>
        )}
      </div>

      <div className="logo-container">
        <div className="logo-core glitch">A</div>
        <h1 className="glitch">
          AEGIS <span>AI</span>
        </h1>
        <p>Behavioral Anti-Cheat System</p>
      </div>

      <div className="progress-bar-container">
        <div className="progress-bar" style={{ width: `${progress}%` }} />
        <span className="progress-text">{progress}%</span>
      </div>
    </div>
  );
}
