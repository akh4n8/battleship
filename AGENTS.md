# 🤖 Battleship Project: Agent Instructions (AGENTS.md)

## 🎯 Project Overview
A modern Android Battleship application built with Kotlin, Jetpack Compose, and pure Room SQLite. It features an advanced tiered bot AI system, historical game tracking, analytics (Heatmaps, Trend Engines), and highly complex, themeable Compose animations.

## 📂 Architecture & Package Map
The project strictly follows Unidirectional Data Flow (UDF) and MVVM architecture.

* **`ai/`**: Bot logic, probability density, and target selection. *(**See `AI_ROADMAP.md`** before modifying).*
* **`ui/` & `widgets/`**: Interactive grids, debug layers, and themeable components reacting to `PlaybackCommand`. *(**See `UI_ANIMATION_RULES.md`** before modifying).*
* **`analytics/`**:
  * `InferenceEngine`: Calculates live win probability using pairwise Z-Scores.
  * `TrendEngine`: Aggregates historical performance metrics.
* **`data/`**: Room Entities (`Game`, `Move`) and DAOs (DataStore is completely removed).
* **`model/`**: Pure DTOs (`BotDecision`, `HeatmapData`) and Enums (`GamePhase`, `PlaybackCommand`, `ShotOutcome`).
* **`viewmodel/`**: `BattleshipViewModel` is the Single Source of Truth.

---

## ⚖️ GAME RULES (STRICT)

### 1. Board & Fleet
* **Grid:** 10x10.
* **Fleet:** 5 ships (lengths: 5, 4, 3, 3, 2). Total pegs = 17.
* **Placement:** Horizontal/Vertical. No overlaps. Adjacency is allowed.

### 2. Turn Mechanics (Continuous Fire)
* **Action:** Firing on previous targets is PROHIBITED.
* **Turn Flow:** A player continues firing as long as they get a **Hit** or a **Sunk** result.
* **Turn End:** A turn only ends on a **Miss**.
* **Max Shots:** Strict technical limit of 100 shots per game.

### 3. Fog of War (Blind Sink)
* **Hidden Identity:** The ship class/length is NEVER revealed to the opponent upon sinking.
* **Hidden Status:** Previous hits on a sunken ship are NOT updated; they remain marked as standard "Hits" in the UI history. Only the final lethal shot is marked "Sunk."

---

## 🛑 STRICT RULES OF ENGAGEMENT (DATA & STATE)

### 1. UI is Dumb, ViewModel is Smart
* Compose UI files MUST NOT calculate game logic or deduce rules.
* **The State Rule:** `BattleshipViewModel` holds the absolute truth. The UI only observes.
* **The UpdatedState Rule:** Use `rememberUpdatedState` inside `InteractiveCanvasGrid` for gestures to ensure callbacks always read the latest data.

### 2. Database & AI Memory (The Snapshot Architecture)
* **Single Source of Truth:** All memory is dynamically calculated and embedded directly into the SQLite `Game` row.
* **The Time Capsule:** When a new game starts, the ViewModel calculates Bayesian priors and immediately serializes them into the `botBrainMetadata` TEXT column. This snapshot is frozen forever.
* **Replay Isolation:** During Replay Mode, the engine strictly loads the `botBrainMetadata` snapshot from that specific historical game, perfectly insulating historical playbacks from future learning.

### 3. AI & Diagnostics Protocol (Mathematical Determinism)
* **Deterministic Parity:** The checkerboard hunting pattern MUST be derived precisely via `gameId % 2`. This prevents visual flickering and layout shifts during replays.
* **Moriarty Confidence Fallback (Strict):** Moriarty V2 utilizes a 7-channel neural network. If the prediction confidence ratio (maxProb / avgProb) falls below 1.3, it MUST fall back to Mycroft's MCMC "Maverick Mode" (preserving the deterministic parity logic).
* **Deterministic Tie-Breaking:** All `random()` calls during AI hunts must use `kotlin.random.Random((gameId * 10000) + moves.size)`. This unbreakable seed guarantees 100% mathematical reproducibility for every single shot in history.
* **Linear Kill Synchronization:** The UI `getLiveHeatmap` must explicitly spike Linear Kill targets to `100` so the visual heatmap aligns perfectly with the bot's target coordinate.

---

## 🛑 STRICT RULES OF ENGAGEMENT (ML PIPELINE)

### 1. Windows Python Execution
* **UTF-8 Requirement:** When running Python training scripts (`train.py`, `model.py`) via PowerShell on Windows, ALWAYS set `$env:PYTHONIOENCODING="utf-8"` before execution to prevent `cp1252` encoding crashes when outputting emojis (e.g. ✅).

### 2. ONNX & TFLite Conversion
* **onnx2tf Syntax:** Do NOT use the deprecated `--overwrite_training_parameters` flag with modern `onnx2tf` versions.
* **TFLite Environment:** When running `onnx2tf` for TFLite conversion, you MUST use `ai-edge-litert` (v2.14.0+) rather than generic `tflite-runtime` or `tensorflow`, or the layer normalizations will fail.

---

## 🛠️ Version Control & Documentation Protocol (STRICT)

* **Documentation Driven Development:** Whenever a major feature, bug fix, or architectural shift is completed, the corresponding `.md` files (e.g., `AGENTS.md`, `AI_ROADMAP.md`) MUST be immediately updated to reflect the new state of the codebase.
* **Branching:** Use descriptive branch names: `feature/name-of-feature` or `fix/bug-description`.
* **Commits:** Commit messages must follow the format `[Component] Summary of changes`. Example: `[AI] Refactored SherlockBot's parity sweep.`
* **Ignored Data:** `.artifacts/` and `.kotlin/` are strictly excluded from the repository.