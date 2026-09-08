# ⚓ Battleship: Multiplatform & AI Research Lab

[![Live Web Demo](https://img.shields.io/badge/Play%20Live-GitHub%20Pages-blue?style=for-the-badge&logo=github)](https://akh4n8.github.io/battleship/)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4?style=for-the-badge&logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

A modern, tactical **Battleship** game and AI research testbed built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, targeting both **WebAssembly (Wasm)** in the browser and native **Android**.

Play directly in your browser with zero installation:  
👉 **[Launch Battleship Live](https://akh4n8.github.io/battleship/)**

---

## ✨ Features

* 🤖 **Tiered AI Bot Hierarchy**: Battle against 5 specialized AI personalities powered by spatial probability, constraint satisfaction, and Monte Carlo algorithms.
* 🕹️ **3 Distinct Game Modes**:
  * **Bot Match**: Challenge custom AI engines in single-player naval duels.
  * **Pass & Play (Hot Seat)**: Play against a friend locally on a single device with screen handoff protection.
  * **Tabletop Companion**: Use the app as a real-time digital radar, heat map visualizer, and win-probability engine for physical plastic Battleship board games.
* 📊 **Deep Analytics & Replay Engine**:
  * **Live Win Probability**: Dynamic pairwise Z-score estimation updated every turn.
  * **Spatial Heatmaps**: Interactive density visualizations displaying bot hunting distributions.
  * **Turn-by-Turn Replay Scrubber**: Rewind, fast-forward, or step through historical matches.
* 📡 **Automated Research Telemetry**:
  * 100% anonymous, privacy-first telemetry pipeline contributing gameplay coordinates directly to a cloud research dataset (Supabase) to train future bot models.
  * In-browser CSV match history export and smart import.
* 🚀 **WebAssembly (Wasm)**: High-performance near-native execution in any modern web browser via Skiko Canvas rendering.

---

## 🧠 The AI Bot Hierarchy

| Bot | Mathematical Approach | Playstyle & Archetype |
| :--- | :--- | :--- |
| **Watson** | Marginal Probability Estimator | Dependable baseline spatial density. Groups targets geometrically but suffers from combinatorial ship shadowing. |
| **Sherlock** | Constraint Satisfaction Problem (CSP) | Analytical solver using deduction sweeps to guarantee a strict mathematical ceiling on wasted shots. |
| **Mycroft** | Monte Carlo Joint-Probability | Executes a 100,000-step zero-allocation MCMC loop using 128-bit bitboards for joint probability calculation. |
| **Adler** | Adaptive Counter-Strategy | Analyzes player tendencies and dynamically shifts between offensive hunting and defensive counter-placement. |

---

## 🛠️ Architecture & Tech Stack

* **Language**: Kotlin 2.2+
* **UI Toolkit**: Compose Multiplatform (Material 3)
* **Targets**:
  * `wasmJs`: Kotlin/Wasm + Skiko Canvas (Web)
  * `android`: Jetpack Compose + Android Gradle Plugin
* **Persistence**:
  * Android: Room SQLite Database
  * Web: Browser `localStorage` with Base64 serialization
* **Cloud Telemetry**: Supabase (PostgreSQL via REST API)
* **CI/CD**: GitHub Actions automated pipeline deploying directly to GitHub Pages

---

## 💻 Local Development

### Prerequisites
* JDK 21+
* Android Studio (Ladybug / Koala) or IntelliJ IDEA Ultimate
* Node.js & Yarn (managed automatically by Gradle for Wasm)

### Running the Web App (Development Server)
```bash
./gradlew wasmJsBrowserDevelopmentRun
```
Open `http://localhost:8080` in your browser.

### Building the Production Web Bundle
```bash
./gradlew :app:wasmJsBrowserDistribution
```
The output will be generated in `app/build/dist/wasmJs/productionExecutable/`.

### Building the Android APK
```bash
./gradlew :app:assembleDebug
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
