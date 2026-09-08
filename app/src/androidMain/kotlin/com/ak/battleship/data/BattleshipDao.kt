package com.ak.battleship.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BattleshipDao : GameRepository {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insertGame(game: Game): Long
    @Update
    override suspend fun updateGame(game: Game)
    @Query("SELECT * FROM games WHERE id = :gameId")
    override suspend fun getGameById(gameId: Int): Game?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insertMove(move: Move)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insertMoves(moves: List<Move>)
    @Query("DELETE FROM moves WHERE id = (SELECT MAX(id) FROM moves WHERE gameId = :gameId AND result NOT IN ('SHIP', 'Carrier', 'Battleship', 'Cruiser', 'Submarine', 'Destroyer'))")
    override suspend fun deleteLastMove(gameId: Int)
    @Delete
    override suspend fun deleteMoves(moves: List<Move>)
    @Query("DELETE FROM games WHERE id = :gameId")
    override suspend fun deleteGameById(gameId: Int)
    @Query("SELECT * FROM games ORDER BY timestamp DESC")
    override fun getAllGames(): Flow<List<Game>>
    @Query("SELECT * FROM games ORDER BY timestamp DESC")
    override suspend fun getAllGamesSync(): List<Game>
    @Query("SELECT * FROM moves ORDER BY gameId, turnNumber ASC")
    override suspend fun getAllMovesSync(): List<Move>
    @Query("SELECT DISTINCT opponentName FROM games")
    override fun getUniqueOpponents(): Flow<List<String>>
    @Query("SELECT * FROM moves WHERE gameId IN (:gameIds)")
    override suspend fun getMovesForGames(gameIds: List<Int>): List<Move>
    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY turnNumber ASC")
    override fun getMovesForGame(gameId: Int): Flow<List<Move>>
    @Query("SELECT * FROM moves WHERE gameId = :gameId ORDER BY turnNumber ASC")
    override suspend fun getMovesForGameSync(gameId: Int): List<Move>
    @Query("SELECT * FROM opponent_profiles WHERE opponentName = :name")
    override suspend fun getProfile(name: String): OpponentProfile?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    override suspend fun insertProfile(profile: OpponentProfile)
    @Query("SELECT DISTINCT playerName FROM games WHERE playerName IS NOT NULL AND playerName != '' ORDER BY playerName COLLATE NOCASE ASC")
    override fun getUniquePlayers(): Flow<List<String>>
}
