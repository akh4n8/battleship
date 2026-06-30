package com.ak.battleship.model

/**
 * CORE DOMAIN: Game State Enum
 * * Represents the current macroscopic phase of the game loop.
 * * It dictates which screens are visible and strictly controls whether the user is
 * allowed to fire weapons or place ships.
 */
enum class GamePhase {
    PLACEMENT,
    BATTLE,
    OPPONENT_SHIPS,
    HANDOFF
}