<div align="center">

# AEGIS AI

<img src="https://readme-typing-svg.herokuapp.com?font=Orbitron&weight=900&size=40&pause=1000&color=FF0A54&center=true&vCenter=true&width=600&height=100&lines=AEGIS+AI+ANTI-CHEAT;Real-Time+Threat+Detection;ML-Powered+Security" alt="Typing SVG" />

<br/>

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Python](https://img.shields.io/badge/Python-3.11+-3776AB?style=for-the-badge&logo=python&logoColor=white)](https://python.org/)
[![License](https://img.shields.io/badge/License-MIT-FF0A54?style=for-the-badge)](LICENSE)

<br/>

<img src="https://user-images.githubusercontent.com/74038190/212284115-f47cd8ff-2ffb-4b04-b5bf-4d1c14c0247f.gif" width="1000">

### *Next-Generation Anti-Cheat System Powered by Machine Learning*

[Features](#-features) • [Installation](#-installation) • [Architecture](#-architecture) • [Tech Stack](#-tech-stack) • [Usage](#-usage)

<br/>

</div>

---

## Overview

**AEGIS AI** is a sophisticated anti-cheat detection system that leverages machine learning and real-time behavioral analysis to identify cheaters in gaming environments. The system monitors mouse movements, keyboard patterns, and player behavior to detect anomalies indicative of cheating software.

<div align="center">
<img src="https://user-images.githubusercontent.com/74038190/225813708-98b745f2-7d22-48cf-9150-083f15b36f93.gif" width="500">
</div>

---

## Features

<table>
<tr>
<td width="50%">

### Real-Time Monitoring
- Live velocity tracking
- Mouse movement analysis
- Keyboard pattern detection
- Multi-device support

</td>
<td width="50%">

### ML-Powered Detection
- Isolation Forest algorithm
- Behavioral anomaly scoring
- Cheat score calculation
- Adaptive threat levels

</td>
</tr>
<tr>
<td width="50%">

### Modern Dashboard
- Live Overwatch monitoring
- Global Intel analytics
- Regional distribution maps
- Top offenders tracking

</td>
<td width="50%">

### Cross-Platform
- Windows desktop app (JavaFX)
- Web-based dashboard
- Python input agent
- REST API integration

</td>
</tr>
</table>

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        AEGIS AI SYSTEM                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐ │
│  │   Python    │    │   Spring    │    │    React Dashboard  │ │
│  │   Agent     │───▶│   Boot      │───▶│    - Live Overwatch │ │
│  │             │    │   Engine    │    │    - Global Intel   │ │
│  │ Input Data  │    │   + ML      │    │    - Analytics      │ │
│  └─────────────┘    └─────────────┘    └─────────────────────┘ │
│        │                  │                      │              │
│        │                  ▼                      │              │
│        │         ┌─────────────┐                 │              │
│        └────────▶│  ML Engine  │◀────────────────┘              │
│                  │  (Sklearn)  │                                │
│                  └─────────────┘                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

<div align="center">

| Layer | Technology |
|-------|------------|
| **Backend** | ![Java](https://img.shields.io/badge/Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=spring&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apache-maven&logoColor=white) |
| **Frontend** | ![React](https://img.shields.io/badge/React-61DAFB?style=flat-square&logo=react&logoColor=black) ![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white) ![TailwindCSS](https://img.shields.io/badge/Tailwind-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white) |
| **ML Engine** | ![Python](https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white) ![scikit-learn](https://img.shields.io/badge/Scikit--Learn-F7931E?style=flat-square&logo=scikit-learn&logoColor=white) |
| **Agent** | ![Python](https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white) ![pynput](https://img.shields.io/badge/pynput-Input_Capture-red?style=flat-square) |
| **Desktop** | ![JavaFX](https://img.shields.io/badge/JavaFX-WebView-007396?style=flat-square&logo=java&logoColor=white) |

</div>

---

## Installation

### Prerequisites

- **Java 17+** - [Download](https://adoptium.net/)
- **Node.js 18+** - [Download](https://nodejs.org/)
- **Python 3.11+** - [Download](https://python.org/)
- **Maven 3.8+** - [Download](https://maven.apache.org/)

### Quick Start

```bash
# Clone the repository
git clone https://github.com/ayushiyer10/AEGIS-AI.git
cd AEGIS-AI

# Start the backend engine
cd engine
mvn spring-boot:run

# In a new terminal, start the frontend (development)
cd anticheat-ui
npm install
npm run dev

# In another terminal, start the Python agent
cd anticheat-agent-python
pip install -r requirements.txt
python input_capture.py
```

### Windows Quick Launch

```batch
# Use the provided batch file
start_aegis.bat
```

---

## Project Structure

```
AEGIS-AI/
├── anticheat-agent-python/     # Python input capture agent
│   ├── input_capture.py        # Mouse/keyboard monitoring
│   ├── feature_builder.py      # Feature extraction
│   └── forward_to_engine.py    # Data transmission
│
├── anticheat-ui/               # React dashboard
│   ├── src/
│   │   ├── App.jsx             # Main application
│   │   ├── GamingDashboard.jsx # Live Overwatch page
│   │   ├── GlobalIntel.jsx     # Global Intel page
│   │   └── SplashScreen.jsx    # Boot animation
│   └── vite.config.js
│
├── engine/                     # Spring Boot backend
│   ├── src/main/java/com/anticheat/
│   │   ├── engine/             # Core engine
│   │   │   ├── controller/     # REST endpoints
│   │   │   ├── service/        # Business logic
│   │   │   └── model/          # Data models
│   │   └── agent/              # Java agent (optional)
│   └── pom.xml
│
├── ml-engine/                  # Machine learning module
│   ├── src/
│   │   ├── detector.py         # Anomaly detection
│   │   └── feature_engineering.py
│   └── model/
│       └── isolation_forest.joblib
│
└── scripts/                    # Utility scripts
    ├── package-windows.ps1
    └── register-agent-startup.ps1
```

---

## Usage

### Dashboard Views

#### Live Overwatch
Real-time monitoring of connected devices with:
- Active agent count
- Cheaters flagged
- Velocity metrics
- Behavioral charts
- Security logs

#### Global Intel
Aggregate intelligence including:
- Regional distribution
- Detection methods breakdown
- 6-week trend analysis
- Top offenders list

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/devices` | GET | List all monitored devices |
| `/api/devices/{id}/live` | GET | Live data for specific device |
| `/api/live` | GET | Current live snapshot |
| `/api/live/snapshots` | GET | Historical snapshots |
| `/api/ingest` | POST | Submit telemetry data |

---

## Detection Methods

<div align="center">

| Method | Description | Indicators |
|--------|-------------|------------|
| **Aimbot** | Artificial aim assistance | High snap rates, unnatural precision |
| **Wallhack** | Seeing through walls | Unusual click patterns, tracking behavior |
| **Speedhack** | Movement speed manipulation | Abnormal velocity readings |
| **Macro** | Automated input sequences | Inhuman click rates, perfect timing |

</div>

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

<img src="https://user-images.githubusercontent.com/74038190/212284115-f47cd8ff-2ffb-4b04-b5bf-4d1c14c0247f.gif" width="1000">

### Built with precision by Ayush Iyer

<img src="https://readme-typing-svg.herokuapp.com?font=Orbitron&size=18&pause=1000&color=22D3EE&center=true&vCenter=true&width=435&lines=Protecting+fair+play;One+detection+at+a+time" alt="Typing SVG" />

<br/>

[![GitHub](https://img.shields.io/badge/GitHub-ayushiyer10-181717?style=for-the-badge&logo=github)](https://github.com/ayushiyer10)

</div>
