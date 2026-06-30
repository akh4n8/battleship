# 🤖 Battleship Project: Agent Instructions (AGENTS.md)

## 🎯 Project Overview
A modern Android Battleship application built with Kotlin, Jetpack Compose, and Room SQLite. It features advanced Monte Carlo/Bayesian bot AI (`MoriartyBot`), historical game tracking, advanced analytics (Heatmaps, Trend Engines), and highly complex, themeable Compose animations.

## 📂 Architecture & Package Map
The project strictly follows Unidirectional Data Flow (UDF) and MVVM architecture.

* **`ai/`**: Bot logic, probability density, and target selection.
    * `TacticalEngine`: Router for all bots.
    * `DeductionEngine`: Shared physics/logic engine for solving board states.
* **`analytics/`**: 
    * `InferenceEngine`: Calculates live win probability using pairwise Z-Scores.
    * `TrendEngine`: Aggregates historical performance metrics.
* **`data/`**: Room Entities (`Game`, `Move`), DAOs, and `AiMemoryDataStore` for Bayesian matrices.
* **`model/`**: Pure DTOs (`BotDecision`, `HeatmapData`) and Enums (`GamePhase`, `PlaybackCommand`, `ShotOutcome`).
* **`ui/`**: 
    * `game/`: Interactive grids, debug layers, and particle systems.
    * `widgets/`: Themeable components (`SubmersibleCameraWidget`, etc.) reacting to `PlaybackCommand`.
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

## 🛑 STRICT RULES OF ENGAGEMENT

### 1. UI is Dumb, ViewModel is Smart
* Compose UI files MUST NOT calculate game logic or deduce rules.
* **The State Rule:** `BattleshipViewModel` holds the absolute truth. The UI only observes.
* **The UpdatedState Rule:** Use `rememberUpdatedState` inside `InteractiveCanvasGrid` for gestures to ensure callbacks always read the latest data.

### 2. The Explicit Command Architecture (ANIMATIONS)
**CRITICAL:** We use a Unified Explicit Command Flow to prevent 1-frame race conditions and "Time-Machine" glitches.
* **The Calculation:** `GameScreen.kt` compares `playbackIndex` against a `remembered` previous index to determine the `PlaybackCommand`.
* **The Sync:** Use an object-based tracker (e.g., `val indexTracker = remember { object { var lastIndex = -1 } }`) to track state transitions synchronously without triggering recompositions.
* **The Widgets:** Widgets MUST only react to the explicit `commandKey` and `command` inside a `LaunchedEffect`.

### 3. Preventing Compose Anti-Patterns
* **The 1-Frame Anti-Flicker (PENDING LOCK):** Always track `processedCommandKey`.
    * *Usage:* `val isPendingAnimation = commandKey != processedCommandKey`.
    * *Rule:* Set `processedCommandKey = commandKey` only AFTER internal states are reset inside the `LaunchedEffect`. This locks the UI to a "clean" state before the next animation begins.
* **Layer Handoff (OVERLAY GUARD):** Use a master boolean (e.g., `isActionOverlayActive`) to suppress "Ambient Layers" (patrols, idle lures) during strikes or intros.
* **Zero-Allocation Canvas:** 
    * Hoist `rememberTextMeasurer()` outside the `Canvas` block.
    * Use static `object` geometry (e.g., `FishGeometry`) to cache `Path()` objects.

### 4. Database & AI Memory
* **Moriarty Matrices:** Offensive/Defensive priors are stored in `AiMemoryDataStore` keyed by `moriarty_offense_$playerName`.
* **Point-in-Time (PIT) Reconstruction:** For historical replays, the ViewModel reconstructs the Bayesian prior using a `limitTimestamp`. This ensures the bot's "brain state" matches its knowledge at the time of that specific match.
* **Psychological Profile Merging:** Priors are built using a 70/30 split: 70% from specific adversarial matches (vs. MoriartyBot) and 30% from general matches (Companion/PassAndPlay). This captures both "Tricking the Bot" and "Natural Habit" behaviors.
* **Normalization Protocol:** 
    - **Offense:** Laplace Smoothing (Base 1.0) and Average-Relative Scaling.
    - **Defense:** Match-Count Normalization (`totalHeat / history.size`) to prevent value explosion over large histories.
* **Brain Snapshots (Metadata):** Historical games store an immutable snapshot of the bot's priors in the `botBrainMetadata` TEXT column.
    - **Format:** Pipe-delimited key-value store: `key1#value1|key2#value2`.
    - **Usage:** Prevents brain corruption if games are deleted and ensures O(1) loading performance.
* **Room Migrations:** Any schema change REQUIRES a migration in `AppDatabase.kt`.

### 5. AI & Diagnostics Protocol
* **The "Eye" Drawer:** The UI includes a debug toolbar for `Heatmaps` and `Structural Diagnostics`.
* **Bot Log:** AI reasoning is displayed via `botLog` on the grid.
* **Deterministic Parity:** `parityOffset` (Checkerboard) must be derived from `gameId % 2`. This prevents visual flickering during replays while maintaining unpredictability across new games.
* **Epsilon-Greedy Defense:** Moriarty ship placement uses a 30% randomness factor (`Random.nextFloat() < 0.30f`) to prevent a human from "solving" its learned defense pattern.

---

## 🧩 Component Dictionary

### `InferenceEngine` (Analytics)
Calculates live win probability using pairwise Z-Scores. Normalizes names to lowercase and handles inverted matchups by flipping the sign. Clamped between 0.01 and 0.99.

### `DeductionEngine` (AI)
The "Shared Brain" that groups hits into ships, calculates `LinearKill` vectors, and supports Monte Carlo simulations for `DeepBlueBot`.

### `PlaybackCommand` (Enum)
* `IDLE`: No active animation.
* `TIME_TRAVEL_BACKWARD`: User scrubbed left or hit "Previous".
* `TIME_TRAVEL_FORWARD`: User skipped multiple turns forward.
* `PLAY_MISS`, `PLAY_HIT`, `PLAY_SUNK`: Live combat triggers.
* `PLAY_WIN_LIVE`: Game-winning shot celebration (No reset to 50%).
* `PLAY_WIN_CINEMATIC`: Loading a finished win from history (Manual sweep from 50%).
* `PLAY_LOSS_CINEMATIC`: Loading a finished loss from history.
* `PLAY_JUMP`: Rapid scrubbing or jump-cuts; used to instantly reset widget state.

---

## 🛠️ The SubmersibleCamera Blueprint (Category 1-4 Specs)
When creating or editing widgets, follow these specific visual rules:
1. **Live Player Hits:** Fish must lock species mid-swipe (No shapeshifting).
2. **Standard Sinks:** Vertical yank with quadratic ease-in; snout must stay attached to the line (`-snoutYOffset`).
3. **Boss Catch (Musky):** Lunge from LEFT (off-screen) to CENTER, thrash violently, and settle into victory patrol.
4. **Boots (Misses):** Perfect symmetry. Player miss (Left, CW tumble). Opponent miss (Right, Flipped, CCW tumble).
5. **Historical Suspense:** Always include a `delay(300)` for win cinematics before the lunge/intro begins.
6. **Ambient Safety:** Hide ambient patrol fish ONLY during the Musky victory loop. During all other strikes, misses, or intros, patrol fish remain visible as background ambience.

---

## 🛠️ Version Control Protocol (STRICT)

### 1. Repository Management
* **Branching:** Use descriptive branch names: `feature/name-of-feature` or `fix/bug-description`.
* **Commits:** Commit messages must follow the format `[Component] Summary of changes`. 
    * *Example:* `[UI] Added SubmersibleCamera particle effects.`
* **Ignored Data:** `.artifacts/` and `.kotlin/` are strictly excluded from the repository.

### 2. When Writing New Code
1. **Respect the Router:** New Widgets must be added to `WidgetTheme` (ViewModel) and `DynamicWinProbabilityWidget` (UI).
2. **Stateless Canvas:** `Canvas` blocks must remain stateless. Logic belongs in `remember` or `LaunchedEffect`.
3. **BackHandler:** Always use `BackHandler` in top-level screens to prevent accidental exits.
