**[⬅️ Play the Full Kotlin Game](../)**


# My Honours Thesis Proposal Overview

When playing Battleship, I play with different rules. In short, ships can be adjacent or "touch", and when a ship is sunk, we only say that is sunk and not which ship it is.

Therefore, the hardest and most interesting part is when a ship sinks, you don't know *which* of your previous hits belonged to it. I call this the **Blind Sink Identity Problem** (we can work on the name).

To solve this, I built two different AI bots to see which thinks better:

### 1. Sherlock: Combinatorial Density Map with Kill Algorithm
- Much more computationally efficient, but biased.
- It looks at the timeline of when it made its hits to perfectly cross out impossible ship locations.

### 2. Mycroft: Pure MCMC
- I believe my algorithm is what you call a Markov Chain Monte Carlo (MCMC) algorithm, but I could be wrong.
- It uses this algorithm to play 100,000 random games in its head in a fraction of a second, finding the absolute most likely spot a ship is hiding.
- It is unbiased and my crude tests have said it is a few moves better than Sherlock on average.

### Try it yourself!
*Play with the interactive debug view below to see how the bots think in real-time.*

**Keyboard Controls:**
*   **[1], [2], [3]:** Load special Ambiguity Presets
*   **[R]:** Randomize the board
*   **[Click]:** Manually fire a shot on the board
*   **[S] or [M]:** Let Sherlock or Mycroft take their best shot
*   **[U]:** Undo the last shot
*   **[H]:** Toggle hiding/showing the ships

<iframe src="./app/index.html" width="1020" height="1020" style="border:none;"></iframe>
