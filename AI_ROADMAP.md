# 🧠 AI Theory & Bot Roadmap (AI_ROADMAP.md)

## 🎯 The Shared Brain (`DeductionEngine`)
The core physics and logic engine underpinning the AI structure. It solves board states mathematically, grouping hits into ships and calculating `LinearKill` vectors. All high-tier bots rely on this engine.

---

## ♟️ Bot Tiering System

### Tier 1: Pure Math (Optimal vs. Random Opponents)
Bots designed to achieve mathematical game-theory efficiency against an opponent playing randomly.
* **`PureDensityBot` (Working):** Basic geometric density mapping.
* **`SherlockBot` (Working):** Game Theory Optimal baseline. Uses `DeductionEngine` to solve board states and hunts via advanced gravitational sweepers.
* **`DeepBlueBot` (WIP / BROKEN):** Monte Carlo search. Currently broken. *Roadmap: Rip out current logic and rebuild using the `DeductionEngine` for simulation.*

### Tier 2: Psychological Profiling (Optimal vs. Human Opponents)
Bots built on top of Tier 1 foundations, but overridden to exploit human biases and habits.
* **`MoriartyBot` (Working - Primary Focus):** An exploitative Bayesian bot built on `SherlockBot`.
    * *Methodology:* Uses psychological profiling matrices (Bayesian priors) built via a 70/30 split (70% specific adversarial matches vs Moriarty, 30% general human matches).
    * *Defense:* Epsilon-Greedy placement (30% randomness factor) to prevent humans from "solving" the learned defense pattern.
    * *Normalization:* Laplace Smoothing (Base 1.0) and Match-Count Normalization.

### Tier 3 & Beyond (Future Roadmap)
* **The "Lucky Genius" Bot:** The conceptual inverse of `MoriartyBot`. An AI designed specifically to lose optimally—making the human player feel like a strategic genius without the game feeling boring or intentionally thrown.
* **Neural Networks:** Future exploration of ML models to supplement psychological profiling.

---

## 🛑 Legacy & Experimental (DO NOT USE FOR ARCHITECTURE)
The following bots are kept strictly for reference and **must not** be used as baselines for new architecture or logic routing.
* **`DensityBot` (Deprecated):** A flawed early attempt at Sherlock's deduction. Kept only as a reference for DeepBlue's future rebuild.
* **`NemesisBot` (Broken):** A failed Reinforcement Learning experiment. Do not route logic here.