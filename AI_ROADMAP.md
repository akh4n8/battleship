# 🧠 AI Theory & Bot Roadmap (AI_ROADMAP.md)

## 🎯 The Shared Brain (`DeductionEngine`)
The core physics and logic engine underpinning the AI structure. It solves board states mathematically as a Constraint Satisfaction Problem (CSP), grouping hits into ships and calculating `LinearKill` vectors. All high-tier bots rely on this engine.

---

## ♟️ Bot Tiering System (The Arthur Conan Doyle Hierarchy)

### Tier 1: The Mathematicians (Optimal vs. Random Opponents)
These algorithms assume a "random" or mathematically perfect opponent and attempt to solve the board using pure spatial reasoning. They do not profile the human player.

* **`WatsonBot` (Working)** *(Formerly: PureDensityBot)*
  * *The Math:* Marginal Probability Estimator.
  * *The Lore:* John Watson is solid, dependable, and relies on standard medical logic. He maps basic geometric density but suffers from combinatorial shadowing, missing the big picture when ships block one another.
* **`SherlockBot` (Working)** * *The Math:* Constraint Satisfaction Solver (CSP).
  * *The Lore:* The ultimate consulting detective. He uses the `DeductionEngine` to solve board states and hunts via advanced gravitational sweepers to guarantee a mathematical ceiling on wasted moves. Blisteringly fast.
* **`MycroftBot` (Working)** *(Formerly: DeepBlueBot)*
  * *The Math:* Monte Carlo Joint-Probability Approximator.
  * *The Lore:* Sherlock's older, smarter brother. He possesses massive computational power, executing a 100,000-step zero-allocation Markov Chain Monte Carlo loop using 128-bit bitboards. He calculates perfect joint-probability density, mathematically guaranteeing no catastrophic late-game failures.

---

### Tier 2: The Profilers (Optimal vs. Human Opponents)
Bots built on top of Tier 1 foundations, but overridden to exploit human biases, habits, and psychological "tells."

* **`AdlerBot` (Working - Primary Focus)** *(Formerly: MoriartyBot)*
  * *The Math:* Empirical Bayesian MAP Estimator.
  * *The Lore:* Irene Adler ("The Woman") famously outsmarted Holmes by anticipating his assumptions. This bot builds a psychological dossier on the player.
  * *Methodology:* Uses Bayesian priors built via a 70/30 split (70% specific adversarial matches vs Adler, 30% general human matches).
  * *Offense:* Uses an exponential multiplier (`Math.pow(humanBias, 3.0)`) to ensure psychological reads successfully overpower the mathematical gravity of the center board.
  * *Defense:* Employs an Inverse Weighted Roulette Wheel with Dynamic Spacing Penalties to distribute ships organically while avoiding the player's favorite firing zones.

---

### Tier 3 & Beyond (Future Roadmap)
The bleeding-edge bots designed to solve advanced game theory concepts and non-stationarity.

* **`The Sovereign Engine` (Future - Python Testing Phase)**
  * *The Math:* Hybrid Ensemble Engine (Complexity-Gated).
  * *The Lore:* The absolute apex of Battleship logic. Fuses Sherlock's instant geometric speed in the early game with Mycroft's joint-probability MCMC squeeze in the late game to achieve true mathematical optimality.
* **`MoriartyBot` (Future)** *(The Neural Network)*
  * *The Math:* Non-Stationary Sequence Predictor (LSTM / Transformer).
  * *The Lore:* The Napoleon of Crime. Where Adler assumes the player's strategy is a fixed trait, Moriarty learns *meta-patterns* and behavioral reactions, predicting exactly when the human will actively change their strategy.
* **`HudsonBot` (Future)** *(The "Lucky Genius")*
  * *The Math:* Inverse Reinforcement Learning (IRL) / Dynamic Difficulty Adjustment.
  * *The Lore:* Sherlock's landlady gently manages the chaos. She intentionally maximizes her own mathematical regret at the exact right moments to ensure the player wins in a way that feels intensely satisfying and hard-fought.

---

## 🛑 Legacy & Experimental (DO NOT USE FOR ARCHITECTURE)
The following bots are kept strictly for reference and **must not** be used as baselines for new architecture or logic routing.
* **`DensityBot` (Deprecated):** A flawed early attempt at Sherlock's deduction. Kept only as a reference for Mycroft's future rebuild.
* **`NemesisBot` (Broken):** A failed Reinforcement Learning experiment. Do not route logic here.