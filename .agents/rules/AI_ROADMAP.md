# 🧠 AI Theory & Bot Roadmap (AI_ROADMAP.md)

## 🎯 The Shared Brain (`DeductionEngine`)
The core physics and logic engine underpinning the AI structure. It solves board states mathematically as a Constraint Satisfaction Problem (CSP), grouping hits into ships and calculating `LinearKill` vectors. All high-tier bots rely on this engine.

---

## ♟️ Bot Tiering System (The Arthur Conan Doyle Hierarchy)

### Tier 1: The Mathematicians (Optimal vs. Random Opponents)
These algorithms assume a "random" or mathematically perfect opponent and attempt to solve the board using pure spatial reasoning. They do not profile the human player.
[BattleshipDao.kt](app/src/main/java/com/ak/battleship/data/BattleshipDao.kt)
* **`WatsonBot` (Working)** *(Formerly: PureDensityBot)*
  * *The Math:* Marginal Probability Estimator.
  * *The Lore:* John Watson is solid, dependable, and relies on standard medical logic. He maps basic geometric density but suffers from combinatorial shadowing, missing the big picture when ships block one another.
* **`SherlockBot` (Working)**
  * *The Math:* Multi-Hypothesis Constraint Satisfaction Solver (CSP) + Exact Combinatorial Target Density + Dynamic Stride Parity.
  * *The Lore:* The ultimate consulting detective. He uses multi-hypothesis chronological CSP deduction to isolate sunken corpses and compute active hits across all tied solutions. Upgraded with **Exact Combinatorial Target Density** (single-ship and joint 2-ship bitboard covers) and **Dynamic Stride Parity** (hunting on Stride-2 checkerboard while the 2-ship is alive, transitioning to Stride-3 diagonal $(r+2c)\%3$ once the 2-ship is confirmed dead). These upgrades reduce shot average from 45.885 down to **~45.33 shots** (**-0.55 shots net improvement**), cutting blowout games ($>60$ shots) by over 32% while maintaining blistering sub-millisecond execution speed.
* **`MycroftBot` (Working)** *(Formerly: DeepBlueBot)*
  * *The Math:* Monte Carlo Joint-Probability Approximator.
  * *The Lore:* Sherlock's older brother. He possesses massive computational power, executing a 100,000-step zero-allocation Markov Chain Monte Carlo loop using 128-bit bitboards. He calculates joint-probability density to minimize catastrophic late-game failures. Achieves **45.790 ± 0.121 shots** (95% CI: [45.55, 46.03]) across 5,000 games, trailing Sherlock, Sherrinford-Apex, and Porlock due to MCMC convergence mixing limits and lack of exact 2-ship joint target covering.
* **`SherrinfordBot` (Working)**
  * *The Math:* Pure Exact Bayesian Joint-Cover Density Field (Zero Heuristics / Zero Linear Assassin).
  * *The Lore:* The eldest Holmes brother. Where Sherlock relies on deductive heuristic sweepers and Linear Assassin line tracing, and Mycroft relies on stochastic MCMC sampling, Sherrinford calculates the mathematically pure exact Bayesian density field across all surviving fleet configurations. Ship kills and touching-ship resolutions emerge organically as pure mathematical consequence from exact combinatorial joint cover.
  * *Architectural Variants:*
    * **`Sherrinford-Apex`:** The synthesized pinnacle engine fusing Multi-Hypothesis Tied CSP Deduction-locked sunk corpses, Dynamic Stride Parity (Stride-2 checkerboard transitioning to Stride-3 diagonal $(r+2c)\%3$ once the 2-ship is eliminated), and Exact 2-Ship Disjoint Joint Targeting ($P_1, P_2$). Achieves **45.209 ± 0.027 shots** across 100,000 paired boards (**-0.640 shots vs Sherlock**, $t = -18.05$, $p < 0.0001$ at $18.05\sigma$, 50.73% head-to-head win rate).
    * **`Sherrinford-Parity`:** Pure Exact Bayes + Dynamic Stride Parity. Achieves **45.502 ± 0.089 shots** (-0.474 shots vs Sherlock, $t = -4.11$, $p < 0.0001$).
    * **`Sherrinford-Deduction`:** Multi-Hypothesis Tied CSP Deduction + Pure Exact Joint Targeting. Achieves **45.665 ± 0.087 shots** (**-0.311 shots vs Sherlock**, $t = -2.72$, $p = 0.0065$) and maintains the **lowest outlier rate** across all engines (2.36% $>60$ shots).
    * **`Sherrinford` (Baseline):** Pure multi-hypothesis Fog of War marginalization. Achieves **45.802 ± 0.028 shots** across 100,000 games (-0.092 shots vs Sherlock, $p = 0.0132$).
* **`PorlockBot` (Working - The Undisputed Project Champion)**
  * *The Math:* Exact Information-Theoretic Bayesian Expected Utility Engine (Shannon Binary Entropy + Spatial Volume Slicing).
  * *The Lore:* Fred Porlock is the pseudonymous cipher informant from Moriarty's syndicate (*The Valley of Fear*). Operating at the intersection of Cryptography and Information Theory, Porlock evaluates candidate hunt coordinates using Claude Shannon's binary information entropy $H(p) = -p \log_2 p - (1-p) \log_2(1-p)$ and radius-2 ocean openness, optimizing Expected Information Utility: $\text{Utility}(r, c) = 2.5 p + 0.35 H(p) + 0.15 \text{Openness}(r, c)$. Outperforms every engine in project history across 100,000 paired boards with a record **45.131 ± 0.027 shots** (**-0.718 shots vs Sherlock**, $t = -20.39$, $p < 0.0001$ at $20.39\sigma$, 51.09% head-to-head win rate; beats Apex by **-0.078 shots**, $t = -3.48$, $p = 0.0005$).

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
* **`MoriartyBot` (Working - V2 & RL)** *(The Neural Network)*
  * *The Math:* Non-Stationary Sequence Predictor (Transformer / TFLite) & Paired-Board PPO Reinforcement Learning.
  * *The Lore:* The Napoleon of Crime. Where Adler assumes the player's strategy is a fixed trait, Moriarty learns *meta-patterns* and behavioral reactions, predicting exactly when the human will actively change their strategy.
  * *Methodology:* Analyzes the player's last 5 games using a 7-channel tensor (Trauma Hits/Misses, Human Hits/Misses, Clustering, Chronology, Spatial). Uses an inverted psychological weighting scale.
  * *Offense:* Feeds the 7 channels into a PyTorch-trained TFLite model. Triggers a Confidence Threshold: if the neural network is unsure (<1.3 ratio), Moriarty drops back into Mycroft's MCMC "Maverick Mode" (enforcing parity for optimal checkerboarding).
  * *PPO RL Upgrade (Mathematical Minimax Alignment):* Fixed the "lottery ticket" aleatoric overfitting by introducing **Paired-Board Regret Minimization** (evaluating agent shots against a paired Sherlock baseline on the exact same hidden board to eliminate board-difficulty variance: $\text{Regret} = \text{Shots}_{\text{baseline}} - \text{Shots}_{\text{agent}}$), **Dense Hypothesis-Pruning Rewards** (rewarding misses proportional to candidate ship placements eliminated), **Strict Fog-of-War Compliance** (only lethal shots marked sunk; previous hits remain `CELL_HIT`), and **Carrier Penalty Fix** (only penalizing misses incurred during kill sequences).
  * *Defense:* Generates a 100-cell density map from the defense TFLite model and uses a Softmax sample to organically drop ships onto the grid, evading player hunting vectors.
* **`HudsonBot` (Future)** *(The "Lucky Genius")*
  * *The Math:* Inverse Reinforcement Learning (IRL) / Dynamic Difficulty Adjustment.
  * *The Lore:* Sherlock's landlady gently manages the chaos. She intentionally maximizes her own mathematical regret at the exact right moments to ensure the player wins in a way that feels intensely satisfying and hard-fought.

---

## 🛑 Legacy & Experimental (DO NOT USE FOR ARCHITECTURE)
The following bots are kept strictly for reference and **must not** be used as baselines for new architecture or logic routing.
* **`DensityBot` (Deprecated):** A flawed early attempt at Sherlock's deduction. Kept only as a reference for Mycroft's future rebuild.
* **`NemesisBot` (Deprecated RL):** Early unconstrained PPO prototype superseded by Moriarty's Paired-Board Regret PPO engine.