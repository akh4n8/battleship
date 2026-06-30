package com.ak.battleship.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DATA LAYER: Database Entities
 * * These data classes define the exact SQLite table schemas for the Room database.
 * * They are strictly used for persistence and should be mapped to Domain Models
 * if complex business logic is required.
 */

@Entity(tableName = "games")
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val opponentName: String,
    val gameMode: String, // "Bot", "PassAndPlay", or "Companion"
    val timestamp: Long = System.currentTimeMillis(),
    val result: String? = null,
    val botBrainMetadata: String? = null // Generic JSON for future-proofing bot priors
)

@Entity(
    tableName = "moves",
    foreignKeys = [
        ForeignKey(
            entity = Game::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE // If a game is deleted, wipe its timeline
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class Move(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameId: Int,
    val isOffense: Boolean,
    val x: Int,
    val y: Int,
    val result: String,
    val isSunk: Boolean = false,
    val turnNumber: Int, // The grouped turn (e.g., Turn 1 could have 3 shots)
    val shotNumber: Int = 0 // The absolute chronological sequence of the shot
)

@Entity(tableName = "opponent_profiles")
data class OpponentProfile(
    @PrimaryKey val opponentName: String,
    val notes: String = "",
    val favoriteStrategy: String = "Unknown"
)