package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {

    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    fun getGameById(id: Long): Flow<GameEntity?> = gameDao.getGameById(id)

    suspend fun getGameByIdOneShot(id: Long): GameEntity? = gameDao.getGameByIdOneShot(id)

    fun getActiveGameSymbolic(): Flow<GameEntity?> = gameDao.getActiveGameSymbolic()

    suspend fun createGame(type: GameType, players: List<String>, targetScore: Int): Long {
        val playersJson = MoshiHelper.toJson(players)
        val game = GameEntity(
            gameType = type.name,
            playersJson = playersJson,
            status = GameStatus.ACTIVE.name,
            targetScore = targetScore,
            roundsJson = MoshiHelper.toJsonRounds(emptyList())
        )
        return gameDao.insertGame(game)
    }

    suspend fun saveRound(gameId: Long, round: GameRound) {
        val game = gameDao.getGameByIdOneShot(gameId) ?: return
        val currentRounds = MoshiHelper.fromJsonRounds(game.roundsJson).toMutableList()
        currentRounds.add(round)

        val updatedRoundsJson = MoshiHelper.toJsonRounds(currentRounds)
        
        // Check if game is completed
        val players = MoshiHelper.fromJsonPlayers(game.playersJson)
        val playerCumulativeScores = calculateCumulativeScores(players.size, currentRounds, game.gameType)
        
        var isCompleted = false
        var winnerName: String? = null

        if (game.gameType == GameType.HEARTS.name) {
            // Hearts game ends when any player reaches or exceeds target score (e.g. 100)
            val reachedTarget = playerCumulativeScores.any { it >= game.targetScore }
            if (reachedTarget) {
                isCompleted = true
                // In Hearts, the player with the LOWEST score wins!
                val minScoreIndex = playerCumulativeScores.indices.minByOrNull { playerCumulativeScores[it] } ?: 0
                winnerName = players.getOrNull(minScoreIndex)
            }
        } else {
            // Spades game ends when any player/team reaches or exceeds target score (e.g. 250 or 500)
            val reachedTarget = playerCumulativeScores.any { it >= game.targetScore }
            if (reachedTarget) {
                isCompleted = true
                // In Spades, the player with the HIGHEST score wins!
                val maxScoreIndex = playerCumulativeScores.indices.maxByOrNull { playerCumulativeScores[it] } ?: 0
                winnerName = players.getOrNull(maxScoreIndex)
            }
        }

        val updatedGame = game.copy(
            roundsJson = updatedRoundsJson,
            status = if (isCompleted) GameStatus.COMPLETED.name else GameStatus.ACTIVE.name,
            winnerName = winnerName
        )
        gameDao.updateGame(updatedGame)
    }

    suspend fun deleteRound(gameId: Long, roundIndex: Int) {
        val game = gameDao.getGameByIdOneShot(gameId) ?: return
        val currentRounds = MoshiHelper.fromJsonRounds(game.roundsJson).toMutableList()
        if (roundIndex in currentRounds.indices) {
            currentRounds.removeAt(roundIndex)
        }

        // Re-evaluate game status & winner
        val updatedRoundsJson = MoshiHelper.toJsonRounds(currentRounds)
        val players = MoshiHelper.fromJsonPlayers(game.playersJson)
        val playerCumulativeScores = calculateCumulativeScores(players.size, currentRounds, game.gameType)

        var isCompleted = false
        var winnerName: String? = null

        if (currentRounds.isNotEmpty()) {
            if (game.gameType == GameType.HEARTS.name) {
                val reachedTarget = playerCumulativeScores.any { it >= game.targetScore }
                if (reachedTarget) {
                    isCompleted = true
                    val minScoreIndex = playerCumulativeScores.indices.minByOrNull { playerCumulativeScores[it] } ?: 0
                    winnerName = players.getOrNull(minScoreIndex)
                }
            } else {
                val reachedTarget = playerCumulativeScores.any { it >= game.targetScore }
                if (reachedTarget) {
                    isCompleted = true
                    val maxScoreIndex = playerCumulativeScores.indices.maxByOrNull { playerCumulativeScores[it] } ?: 0
                    winnerName = players.getOrNull(maxScoreIndex)
                }
            }
        }

        val updatedGame = game.copy(
            roundsJson = updatedRoundsJson,
            status = if (isCompleted) GameStatus.COMPLETED.name else GameStatus.ACTIVE.name,
            winnerName = winnerName
        )
        gameDao.updateGame(updatedGame)
    }

    suspend fun updateGameStatus(gameId: Long, status: GameStatus) {
        val game = gameDao.getGameByIdOneShot(gameId) ?: return
        gameDao.updateGame(game.copy(status = status.name))
    }

    suspend fun deleteGame(id: Long) = gameDao.deleteGame(id)

    suspend fun deleteAllHistory() = gameDao.deleteAllGames()

    // --- Player Profile Methods ---

    val allPlayerProfiles: Flow<List<PlayerProfile>> = gameDao.getAllPlayerProfiles()

    suspend fun getPlayerProfileByName(name: String): PlayerProfile? = gameDao.getPlayerProfileByName(name)

    suspend fun savePlayerProfile(profile: PlayerProfile) = gameDao.insertPlayerProfile(profile)

    suspend fun deletePlayerProfile(name: String) = gameDao.deletePlayerProfile(name)

    companion object {
        fun calculateCumulativeScores(playerCount: Int, rounds: List<GameRound>, gameType: String): List<Int> {
            val cumulative = MutableList(playerCount) { 0 }
            for (round in rounds) {
                for (i in 0 until playerCount) {
                    cumulative[i] += round.scores.getOrElse(i) { 0 }
                }
            }
            return cumulative
        }

        fun calculateCumulativeBags(playerCount: Int, rounds: List<GameRound>): List<Int> {
            val cumulativeBags = MutableList(playerCount) { 0 }
            for (round in rounds) {
                if (round.spadesDetails != null) {
                    for (i in 0 until playerCount) {
                        val bag = round.spadesDetails.getOrNull(i)?.bagsGained ?: 0
                        cumulativeBags[i] += bag
                    }
                }
            }
            return cumulativeBags
        }
    }
}

// A simple local Moshi helper to avoid type adapter construction inline and prevent dependency bottlenecks
object MoshiHelper {
    private val moshi: Moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    private val stringListAdapter = moshi.adapter<List<String>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
    )

    private val roundListAdapter = moshi.adapter<List<GameRound>>(
        com.squareup.moshi.Types.newParameterizedType(List::class.java, GameRound::class.java)
    )

    fun toJson(list: List<String>): String = stringListAdapter.toJson(list)
    fun fromJsonPlayers(json: String): List<String> = stringListAdapter.fromJson(json) ?: emptyList()

    fun toJsonRounds(list: List<GameRound>): String = roundListAdapter.toJson(list)
    fun fromJsonRounds(json: String): List<GameRound> = roundListAdapter.fromJson(json) ?: emptyList()
}
