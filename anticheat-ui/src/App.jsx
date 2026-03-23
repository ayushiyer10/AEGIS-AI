import { useState } from "react";
import { AnimatePresence, m } from "framer-motion";
import { Activity, Target } from "lucide-react";
import SplashScreen from "./SplashScreen";
import GamingDashboard from "./GamingDashboard";
import GlobalIntel from "./GlobalIntel";
import "./App.css";

export default function App() {
  const [booted, setBooted] = useState(false);
  const [activeView, setActiveView] = useState("overwatch");

  const isDesktopLite =
    typeof window !== "undefined" &&
    new URLSearchParams(window.location.search).get("desktop") === "1";

  if (!booted) {
    return <SplashScreen onFinish={() => setBooted(true)} />;
  }

  const contentVariants = {
    initial: { opacity: 0, y: 12 },
    animate: { opacity: 1, y: 0 },
    exit: { opacity: 0, y: -12 }
  };

  return (
    <div className={`app-root ${isDesktopLite ? "desktop-lite" : ""}`}>
      {/* SHARED SIDEBAR â€” never re-renders on page switch */}
      <aside className="sidebar">
        <div className="logo">
          <div className="logo-badge">A</div>
          <h1>AEGIS AI</h1>
        </div>

        <nav className="nav">
          <div
            className={`nav-item ${activeView === "overwatch" ? "active" : ""}`}
            onClick={() => setActiveView("overwatch")}
          >
            <Activity size={18} /> Live Overwatch
          </div>
          <div
            className={`nav-item ${activeView === "intel" ? "active" : ""}`}
            onClick={() => setActiveView("intel")}
          >
            <Target size={18} /> Global Intel
          </div>
        </nav>

      </aside>

      {/* MAIN CONTENT AREA */}
      <main className="main">
        {isDesktopLite ? (
          <div className="page-stack">
            <section className={`page-content ${activeView === "overwatch" ? "active" : "inactive"}`}>
              <GamingDashboard isActive={activeView === "overwatch"} />
            </section>
            <section className={`page-content ${activeView === "intel" ? "active" : "inactive"}`}>
              <GlobalIntel isActive={activeView === "intel"} />
            </section>
          </div>
        ) : (
          <AnimatePresence mode="wait">
            {activeView === "overwatch" ? (
              <m.div
                key="overwatch"
                className="page-content"
                initial="initial"
                animate="animate"
                exit="exit"
                variants={contentVariants}
                transition={{ duration: 0.25, ease: "easeInOut" }}
              >
                <GamingDashboard />
              </m.div>
            ) : (
              <m.div
                key="intel"
                className="page-content"
                initial="initial"
                animate="animate"
                exit="exit"
                variants={contentVariants}
                transition={{ duration: 0.25, ease: "easeInOut" }}
              >
                <GlobalIntel />
              </m.div>
            )}
          </AnimatePresence>
        )}
      </main>
    </div>
  );
}
