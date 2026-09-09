package com.ak.battleship.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.browser.window
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class DummyRepository : GameRepository {
    private var nextGameId = 1
    private var nextMoveId = 1

    private val games = MutableStateFlow<Map<Int, Game>>(emptyMap())
    private val moves = MutableStateFlow<List<Move>>(emptyList())
    private val profiles = MutableStateFlow<Map<String, OpponentProfile>>(emptyMap())

    init {
        loadFromStorage()
    }

    private fun b64(s: String): String = Base64.encode(s.encodeToByteArray())
    private fun unb64(s: String): String = Base64.decode(s).decodeToString()

    private fun saveGamesToStorage() {
        val gamesStr = games.value.values.joinToString("||") { g ->
            "${g.id}|${b64(g.playerName)}|${b64(g.opponentName)}|${b64(g.gameMode)}|${g.timestamp}|${g.result?.let { b64(it) } ?: ""}|${g.botBrainMetadata?.let { b64(it) } ?: ""}"
        }
        window.localStorage.setItem("battleship_games", gamesStr)
    }

    private fun saveMovesToStorage() {
        val movesStr = moves.value.joinToString("||") { m ->
            "${m.id}|${m.gameId}|${m.isOffense}|${m.x}|${m.y}|${b64(m.result)}|${m.isSunk}|${m.turnNumber}|${m.shotNumber}"
        }
        window.localStorage.setItem("battleship_moves", movesStr)
    }

    private fun saveProfilesToStorage() {
        val profilesStr = profiles.value.values.joinToString("||") { p ->
            "${b64(p.opponentName)}|${b64(p.notes)}|${b64(p.favoriteStrategy)}"
        }
        window.localStorage.setItem("battleship_profiles", profilesStr)
    }

    private fun loadFromStorage() {
        try {
            val gamesStr = window.localStorage.getItem("battleship_games") ?: ""
            if (gamesStr.isNotEmpty()) {
                val parsedGames = gamesStr.split("||").map { row ->
                    val p = row.split("|")
                    Game(
                        id = p[0].toInt(),
                        playerName = unb64(p[1]),
                        opponentName = unb64(p[2]),
                        gameMode = unb64(p[3]),
                        timestamp = p[4].toLong(),
                        result = p[5].takeIf { it.isNotEmpty() }?.let { unb64(it) },
                        botBrainMetadata = p[6].takeIf { it.isNotEmpty() }?.let { unb64(it) }
                    )
                }
                games.value = parsedGames.associateBy { it.id }
                nextGameId = (parsedGames.maxOfOrNull { it.id } ?: 0) + 1
            }

            val movesStr = window.localStorage.getItem("battleship_moves") ?: ""
            if (movesStr.isNotEmpty()) {
                val parsedMoves = movesStr.split("||").map { row ->
                    val p = row.split("|")
                    Move(
                        id = p[0].toInt(),
                        gameId = p[1].toInt(),
                        isOffense = p[2].toBoolean(),
                        x = p[3].toInt(),
                        y = p[4].toInt(),
                        result = unb64(p[5]),
                        isSunk = p[6].toBoolean(),
                        turnNumber = p[7].toInt(),
                        shotNumber = p[8].toInt()
                    )
                }
                moves.value = parsedMoves
                nextMoveId = (parsedMoves.maxOfOrNull { it.id } ?: 0) + 1
            }

            val profilesStr = window.localStorage.getItem("battleship_profiles") ?: ""
            if (profilesStr.isNotEmpty()) {
                val parsedProfiles = profilesStr.split("||").map { row ->
                    val p = row.split("|")
                    OpponentProfile(
                        opponentName = unb64(p[0]),
                        notes = unb64(p[1]),
                        favoriteStrategy = unb64(p[2])
                    )
                }
                profiles.value = parsedProfiles.associateBy { it.opponentName }
            }
        } catch (e: Exception) {
            println("Error loading from local storage: ${e.message}")
        }
    }

    override suspend fun insertGame(game: Game): Long {
        val id = if (game.id == 0) nextGameId++ else game.id
        val newGame = game.copy(id = id)
        games.value = games.value + (id to newGame)
        saveGamesToStorage()
        return id.toLong()
    }

    override suspend fun updateGame(game: Game) {
        games.value = games.value + (game.id to game)
        saveGamesToStorage()
    }

    override suspend fun getGameById(gameId: Int): Game? {
        return games.value[gameId]
    }

    override suspend fun insertMove(move: Move) {
        val id = if (move.id == 0) nextMoveId++ else move.id
        val newMove = move.copy(id = id)
        moves.value = moves.value + newMove
        
        // Optimize Wasm performance: Append to localStorage instead of full rewrite
        val existing = window.localStorage.getItem("battleship_moves") ?: ""
        val m = newMove
        val moveStr = "${m.id}|${m.gameId}|${m.isOffense}|${m.x}|${m.y}|${b64(m.result)}|${m.isSunk}|${m.turnNumber}|${m.shotNumber}"
        val newMovesStr = if (existing.isEmpty()) moveStr else "$existing||$moveStr"
        window.localStorage.setItem("battleship_moves", newMovesStr)
    }

    override suspend fun insertMoves(movesList: List<Move>) {
        val processed = movesList.map { 
            val id = if (it.id == 0) nextMoveId++ else it.id
            it.copy(id = id)
        }
        this.moves.value = this.moves.value + processed
        
        // Optimize Wasm performance: Append to localStorage instead of full rewrite
        val existing = window.localStorage.getItem("battleship_moves") ?: ""
        val appendStr = processed.joinToString("||") { m ->
            "${m.id}|${m.gameId}|${m.isOffense}|${m.x}|${m.y}|${b64(m.result)}|${m.isSunk}|${m.turnNumber}|${m.shotNumber}"
        }
        val newMovesStr = if (existing.isEmpty()) appendStr else if (appendStr.isNotEmpty()) "$existing||$appendStr" else existing
        window.localStorage.setItem("battleship_moves", newMovesStr)
    }

    override suspend fun deleteLastMove(gameId: Int) {
        val lastMove = moves.value.filter { it.gameId == gameId }.maxByOrNull { it.id }
        if (lastMove != null) {
            moves.value = moves.value.filter { it.id != lastMove.id }
            saveMovesToStorage()
        }
    }

    override suspend fun deleteMoves(moves: List<Move>) {
        val idsToRemove = moves.map { it.id }.toSet()
        this.moves.value = this.moves.value.filter { it.id !in idsToRemove }
        saveMovesToStorage()
    }

    override suspend fun deleteGameById(gameId: Int) {
        games.value = games.value - gameId
        moves.value = moves.value.filter { it.gameId != gameId }
        saveGamesToStorage()
        saveMovesToStorage()
    }

    override fun getAllGames(): Flow<List<Game>> = games.map { it.values.toList() }

    override suspend fun getAllGamesSync(): List<Game> = games.value.values.toList()

    override suspend fun getAllMovesSync(): List<Move> = moves.value

    override fun getUniqueOpponents(): Flow<List<String>> = games.map { gamesMap ->
        gamesMap.values.map { it.opponentName }.distinct()
    }

    override suspend fun getMovesForGames(gameIds: List<Int>): List<Move> {
        val set = gameIds.toSet()
        return moves.value.filter { it.gameId in set }
    }

    override fun getMovesForGame(gameId: Int): Flow<List<Move>> = moves.map { list ->
        list.filter { it.gameId == gameId }
    }

    override suspend fun getMovesForGameSync(gameId: Int): List<Move> =
        moves.value.filter { it.gameId == gameId }

    override suspend fun getProfile(name: String): OpponentProfile? = profiles.value[name]

    override suspend fun insertProfile(profile: OpponentProfile) {
        profiles.value = profiles.value + (profile.opponentName to profile)
        saveProfilesToStorage()
    }

    override fun getUniquePlayers(): Flow<List<String>> = games.map { gamesMap ->
        gamesMap.values.map { it.playerName }.distinct()
    }
}
