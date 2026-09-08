package com.ak.battleship.data

import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun insertGame(game: Game): Long
    suspend fun updateGame(game: Game)
    suspend fun getGameById(gameId: Int): Game?
    suspend fun insertMove(move: Move)
    suspend fun insertMoves(moves: List<Move>)
    suspend fun deleteLastMove(gameId: Int)
    suspend fun deleteMoves(moves: List<Move>)
    suspend fun deleteGameById(gameId: Int)
    fun getAllGames(): Flow<List<Game>>
    suspend fun getAllGamesSync(): List<Game>
    suspend fun getAllMovesSync(): List<Move>
    fun getUniqueOpponents(): Flow<List<String>>
    suspend fun getMovesForGames(gameIds: List<Int>): List<Move>
    fun getMovesForGame(gameId: Int): Flow<List<Move>>
    suspend fun getMovesForGameSync(gameId: Int): List<Move>
    suspend fun getProfile(name: String): OpponentProfile?
    suspend fun insertProfile(profile: OpponentProfile)
    fun getUniquePlayers(): Flow<List<String>>
}
