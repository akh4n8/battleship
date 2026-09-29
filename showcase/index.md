**[⬅️ Play the Full Kotlin Game](../)**


# My Honours Thesis: The Blind Sink Identity Problem

When playing Battleship, the hardest part is when a ship sinks, but you don't know *which* of your previous hits belonged to it. I call this the **Blind Sink Identity Problem**.

To solve this, I built two different AI bots to see which thinks better:

### 1. Sherlock: Rule-Based Elimination Engine
Sherlock uses the strict rules of the game. It looks at the timeline of when you made your hits to perfectly cross out impossible ship locations.

### 2. Mycroft: MCMC
Mycroft is a bit different. It uses a Markov Chain Monte Carlo (MCMC) algorithm to play 100,000 random games in its head in a fraction of a second, finding the absolute most likely spot a ship is hiding.

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
