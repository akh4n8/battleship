package com.ak.battleship.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DATA LAYER: Data Access Object (DAO)
 * * The exclusive bridge between the Kotlin engines and the SQLite database.
 * * Contains pre-compiled SQL queries to guarantee type safety and prevent SQL injection.
 */
@Dao
interface BattleshipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: Game): Long

    @Update
    suspend fun updateGame(game: Game)

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: Int): Game?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMove(move: Move)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoves(moves: List<Move>)

    /**
     * Deletes the absolute last combat move.
     * Safely ignores Turn 0 ship setup coordinates.
     */
    @Query("""
        DELETE FROM moves 
        WHERE id = (
            SELECT MAX(id) FROM moves 
            WHERE gameId = :gameId 
              AND result NOT IN ('SHIP', 'Carrier', 'Battleship', 'Cruiser', 'Submarine', 'Destroyer')
        )
    """)
    suspend fun deleteLastMove(gameId: Int)

    @Delete
    suspend fun deleteMoves(moves: List<Move>)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: Int)

    // --- READ OPERATIONS ---

    @Query("SELECT * FROM games ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<Game>>

    @Query("SELECT * FROM games ORDER BY timestamp DESC")
    suspend fun getAllGamesSync(): List<Game>

    @Query("SELECT * FROM moves ORDER BY gameId, turnNumber ASC")
    suspend fun getAllMovesSync(): List<Move>

    @Query("SELECT DISTINCT opponentName FROM games")
    fun getUniqueOpponents(): Flow<List<String>>

    @Query("SELECT * FROM moves WHERE gameId IN (:gameIds)")
    suspend fun getMovesForGames(gameIds: List<Int>): List<Move>

    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY turnNumber ASC")
    fun getMovesForGame(gameId: Int): Flow<List<Move>>

    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY turnNumber ASC")
    suspend fun getMovesForGameSync(gameId: Int): List<Move>

    @Query("SELECT * FROM opponent_profiles WHERE opponentName = :name")
    suspend fun getProfile(name: String): OpponentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: OpponentProfile)

    @Query("SELECT DISTINCT playerName FROM games WHERE playerName IS NOT NULL AND playerName != '' ORDER BY playerName COLLATE NOCASE ASC")
    fun getUniquePlayers(): Flow<List<String>>
}