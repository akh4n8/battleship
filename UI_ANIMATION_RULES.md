# 🎨 UI & Animation Guardrails (UI_ANIMATION_RULES.md)

## 🏛️ The Reactive Connection (UI to ViewModel)
The UI is a purely reactive presentation layer. It contains ZERO game logic or mathematical deductions.
* **The Orchestrator:** `GameScreen.kt` is the only component that should directly observe `StateFlow` variables (via `collectAsState()`) from `BattleshipViewModel`.
* **The Dumb Widgets:** Sub-components and widgets (like `InteractiveCanvasGrid` or `SubmersibleCameraWidget`) MUST be stateless. They receive primitive data, outcomes, and callback lambdas `() -> Unit` from `GameScreen.kt`. They never interact with the ViewModel directly.
* **The `rememberUpdatedState` Rule:** When passing callbacks deeply into gesture detectors (e.g., `detectTapGestures` inside `InteractiveCanvasGrid`), you MUST wrap the lambda in `rememberUpdatedState`. This ensures Compose always executes the latest closure without forcing unnecessary recompositions.
* **System Back Gesture:** Top-level screens MUST implement `BackHandler { viewModel.pauseGame() }` to intercept Android edge-swipes, ensuring background coroutines and AI loops are severed cleanly.

---

## 🎯 The Explicit Command Architecture (ANIMATIONS)
**CRITICAL:** We use a Unified Explicit Command Flow to prevent 1-frame race conditions and "Time-Machine" glitches.
* **The Calculation:** Widgets do NOT compute their own state. `GameScreen.kt` computes the `PlaybackCommand` based on `playbackIndex` (Replay Mode) or `widgetShotTriggerKey` (Live Mode) and passes the command down.
* **The Sync:** Use an object-based tracker (e.g., `val indexTracker = remember { object { var lastIndex = -1 } }`) in `GameScreen` to track state transitions synchronously without triggering recompositions.
* **The Widgets:** Widgets MUST only react to the explicit `commandKey` and `command` inside a `LaunchedEffect`.

## 🧩 PlaybackCommand Dictionary
* `IDLE`: No active animation.
* `TIME_TRAVEL_BACKWARD`: User scrubbed left or hit "Previous".
* `TIME_TRAVEL_FORWARD`: User skipped multiple turns forward.
* `PLAY_MISS`, `PLAY_HIT`, `PLAY_SUNK`: Live combat triggers.
* `PLAY_WIN_LIVE`: Game-winning shot celebration (No reset to 50%).
* `PLAY_WIN_CINEMATIC`: Loading a finished win from history (Manual sweep from 50%).
* `PLAY_LOSS_CINEMATIC`: Loading a finished loss from history.
* `PLAY_JUMP`: Rapid scrubbing or jump-cuts; used to instantly reset widget state.

---

## 🛑 Preventing Compose Anti-Patterns
* **The 1-Frame Anti-Flicker (PENDING LOCK):** Always track `processedCommandKey`.
    * *Usage:* `val isPendingAnimation = commandKey != processedCommandKey`.
    * *Rule:* Set `processedCommandKey = commandKey` only AFTER internal states are reset inside the `LaunchedEffect`. This locks the UI to a "clean" state before the next animation begins.
* **Layer Handoff (OVERLAY GUARD):** Use a master boolean (e.g., `isActionOverlayActive`) to suppress "Ambient Layers" (patrols, idle lures) during strikes or intros.
* **Zero-Allocation Canvas:**
    * Hoist `rememberTextMeasurer()` outside the `Canvas` block.
    * Use static `object` geometry (e.g., `FishGeometry`) to cache `Path()` objects.

---

## 📊 The 7-Stage Probability Matrix (STRICT)
Every new widget MUST map its visual state (colors, text, scaling, or assets) to the exact 7-stage probability thresholds below. Use a single `when` block evaluating the `animatedProb.value`.

* **Stage 1 (Terminal Victory):** `animatedProb >= 1.0f` (100%)
    * *Theme:* Grand Finale, Overwhelming Success, Celebration.
* **Stage 2 (Dominating):** `animatedProb > 0.85f` (86% - 99%)
    * *Theme:* Complete control, high energy, opponent panicking.
* **Stage 3 (Advantage):** `animatedProb > 0.60f` (61% - 85%)
    * *Theme:* Steady, confident, holding the line.
* **Stage 4 (Dead Heat):** `animatedProb > 0.40f` (41% - 60%)
    * *Theme:* Neutral, tense, suspenseful.
* **Stage 5 (Disadvantage):** `animatedProb > 0.15f` (16% - 40%)
    * *Theme:* Taking heavy fire, losing harmony, misdirection.
* **Stage 6 (Critical):** `animatedProb > 0f` (1% - 15%)
    * *Theme:* Imminent failure, massive damage, trick exposed.
* **Stage 7 (Terminal Defeat):** `else` (0%)
    * *Theme:* Total annihilation, system offline, vanished.

**Implementation Rule:** Never map states using generic integers (e.g., `1 to 7`). Always evaluate against the precise floating-point thresholds to ensure perfect synchronization with the `InferenceEngine`'s output.

---

## 🛠️ The SubmersibleCamera Blueprint (Category 1-4 Specs)
When creating or editing widgets, follow these specific visual rules:
1. **Live Player Hits:** Fish must lock species mid-swipe (No shapeshifting).
2. **Standard Sinks:** Vertical yank with quadratic ease-in; snout must stay attached to the line (`-snoutYOffset`).
3. **Boss Catch (Musky):** Lunge from LEFT (off-screen) to CENTER, thrash violently, and settle into victory patrol.
4. **Boots (Misses):** Perfect symmetry. Player miss (Left, CW tumble). Opponent miss (Right, Flipped, CCW tumble).
5. **Historical Suspense:** Always include a `delay(300)` for win cinematics before the lunge/intro begins.
6. **Ambient Safety:** Hide ambient patrol fish ONLY during the Musky victory loop. During all other strikes, misses, or intros, patrol fish remain visible as background ambience.