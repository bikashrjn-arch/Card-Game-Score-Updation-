package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        // Pre-populate some fun profiles if none exist
        viewModelScope.launch {
            repository.allPlayerProfiles.first().let { current ->
                if (current.isEmpty()) {
                    repository.savePlayerProfile(PlayerProfile("Bikash", "#38BDF8", "🤠"))
                    repository.savePlayerProfile(PlayerProfile("Alice", "#F43F5E", "🦊"))
                    repository.savePlayerProfile(PlayerProfile("Dave", "#10B981", "🤖"))
                    repository.savePlayerProfile(PlayerProfile("Carol", "#A855F7", "👑"))
                }
            }
        }
    }

    // Expose all player profiles
    val allPlayerProfiles: StateFlow<List<PlayerProfile>> = repository.allPlayerProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePlayerProfile(name: String, colorHex: String, avatarEmoji: String) {
        viewModelScope.launch {
            repository.savePlayerProfile(
                PlayerProfile(
                    name = name.trim(),
                    colorHex = colorHex,
                    avatarEmoji = avatarEmoji
                )
            )
        }
    }

    fun deletePlayerProfile(name: String) {
        viewModelScope.launch {
            repository.deletePlayerProfile(name)
        }
    }

    // Expose all games
    val allGames: StateFlow<List<GameEntity>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expose active game
    val activeGame: StateFlow<GameEntity?> = repository.getActiveGameSymbolic()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current viewed/played game
    private val _currentGameId = MutableStateFlow<Long?>(null)
    val currentGame: StateFlow<GameEntity?> = _currentGameId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getGameById(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Inputs State for Entry ---
    // Player names for new game setup
    private val _setupPlayersList = MutableStateFlow(listOf("Player 1", "Player 2", "Player 3", "Player 4"))
    val setupPlayersList: StateFlow<List<String>> = _setupPlayersList.asStateFlow()

    fun updateSetupPlayerName(index: Int, name: String) {
        val currentList = _setupPlayersList.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = name
            _setupPlayersList.value = currentList
        }
    }

    fun addSetupPlayer() {
        val currentList = _setupPlayersList.value.toMutableList()
        if (currentList.size < 6) {
            currentList.add("Player ${currentList.size + 1}")
            _setupPlayersList.value = currentList
        }
    }

    fun removeSetupPlayer(index: Int) {
        val currentList = _setupPlayersList.value.toMutableList()
        if (currentList.size > 2) {
            currentList.removeAt(index)
            _setupPlayersList.value = currentList
        }
    }

    // --- Actions ---
    fun selectGame(gameId: Long?) {
        _currentGameId.value = gameId
    }

    fun startNewGame(type: GameType, targetScore: Int, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val players = _setupPlayersList.value.filter { it.isNotBlank() }
            val id = repository.createGame(type, players, targetScore)
            selectGame(id)
            onSuccess(id)
        }
    }

    fun deleteGame(gameId: Long) {
        viewModelScope.launch {
            repository.deleteGame(gameId)
            if (_currentGameId.value == gameId) {
                _currentGameId.value = null
            }
        }
    }

    fun deleteAllHistory() {
        viewModelScope.launch {
            repository.deleteAllHistory()
            _currentGameId.value = null
        }
    }

    fun deleteRound(gameId: Long, roundIndex: Int) {
        viewModelScope.launch {
            repository.deleteRound(gameId, roundIndex)
        }
    }

    fun resumeActiveGame() {
        activeGame.value?.let {
            selectGame(it.id)
        }
    }

    fun finishGameManual(gameId: Long) {
        viewModelScope.launch {
            repository.updateGameStatus(gameId, GameStatus.COMPLETED)
        }
    }

    // --- Dynamic Calculations for Hearts Entry ---
    // List of scores for active entry in Hearts
    private val _heartsEntryScores = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val heartsEntryScores: StateFlow<Map<Int, Int>> = _heartsEntryScores.asStateFlow()

    // Hearts toggle for "Shoot the Moon"
    private val _heartsShooterIdx = MutableStateFlow<Int?>(null)
    val heartsShooterIdx: StateFlow<Int?> = _heartsShooterIdx.asStateFlow()

    // Hearts mode for moon shooting scenario
    private val _heartsMoonPenalizeOthers = MutableStateFlow(true) // true = add 26 to others, false = subtract 26 from shooter
    val heartsMoonPenalizeOthers: StateFlow<Boolean> = _heartsMoonPenalizeOthers.asStateFlow()

    fun updateHeartsScore(playerIndex: Int, score: Int) {
        val currentMap = _heartsEntryScores.value.toMutableMap()
        currentMap[playerIndex] = maxOf(0, score)
        _heartsEntryScores.value = currentMap
    }

    fun incrementHeartsScore(playerIndex: Int, amount: Int) {
        val currentScore = _heartsEntryScores.value[playerIndex] ?: 0
        updateHeartsScore(playerIndex, currentScore + amount)
    }

    fun shootTheMoon(playerIndex: Int) {
        if (_heartsShooterIdx.value == playerIndex) {
            // Unselect
            _heartsShooterIdx.value = null
            _heartsEntryScores.value = emptyMap()
        } else {
            // Apply shooting the moon setup
            _heartsShooterIdx.value = playerIndex
            val playerCount = currentGame.value?.let { MoshiHelper.fromJsonPlayers(it.playersJson).size } ?: 4
            val currentMap = mutableMapOf<Int, Int>()
            if (_heartsMoonPenalizeOthers.value) {
                for (i in 0 until playerCount) {
                    currentMap[i] = if (i == playerIndex) 0 else 26
                }
            } else {
                for (i in 0 until playerCount) {
                    currentMap[i] = if (i == playerIndex) -26 else 0
                }
            }
            _heartsEntryScores.value = currentMap
        }
    }

    fun toggleMoonScoringOption() {
        _heartsMoonPenalizeOthers.value = !_heartsMoonPenalizeOthers.value
        // If someone is currently shooting, update values
        _heartsShooterIdx.value?.let { idx ->
            val playerCount = currentGame.value?.let { MoshiHelper.fromJsonPlayers(it.playersJson).size } ?: 4
            val currentMap = mutableMapOf<Int, Int>()
            if (_heartsMoonPenalizeOthers.value) {
                for (i in 0 until playerCount) {
                    currentMap[i] = if (i == idx) 0 else 26
                }
            } else {
                for (i in 0 until playerCount) {
                    currentMap[i] = if (i == idx) -26 else 0
                }
            }
            _heartsEntryScores.value = currentMap
        }
    }

    fun resetHeartsEntry() {
        _heartsEntryScores.value = emptyMap()
        _heartsShooterIdx.value = null
    }

    fun submitHeartsRound(gameId: Long) {
        val game = currentGame.value ?: return
        val players = MoshiHelper.fromJsonPlayers(game.playersJson)
        val roundNumber = MoshiHelper.fromJsonRounds(game.roundsJson).size + 1

        val finalScores = List(players.size) { index ->
            _heartsEntryScores.value[index] ?: 0
        }

        // Create hearts details
        val details = players.mapIndexed { index, name ->
            val score = finalScores[index]
            HeartsPlayerRoundDetail(
                playerName = name,
                scoreGained = score,
                shotTheMoon = index == _heartsShooterIdx.value,
                queenOfSpadesTaken = score >= 13 && index != _heartsShooterIdx.value,
                heartsTaken = if (index == _heartsShooterIdx.value) 0 else maxOf(0, score) % 13
            )
        }

        val gameRound = GameRound(
            roundNumber = roundNumber,
            scores = finalScores,
            heartsDetails = details
        )

        viewModelScope.launch {
            repository.saveRound(gameId, gameRound)
            resetHeartsEntry()
        }
    }

    // --- Dynamic Calculations for Spades Entry ---
    // Map of PlayerIndex -> Bid string (-1/Nil = "Nil", -2/Blind = "Blind Nil", otherwise number of tricks)
    private val _spadesBids = MutableStateFlow<Map<Int, Int>>(emptyMap()) // playerIndex -> bid integer. Nil is -1, Blind Nil is -2, values 0..13
    val spadesBids: StateFlow<Map<Int, Int>> = _spadesBids.asStateFlow()

    private val _spadesTricks = MutableStateFlow<Map<Int, Int>>(emptyMap()) // playerIndex -> tricks won
    val spadesTricks: StateFlow<Map<Int, Int>> = _spadesTricks.asStateFlow()

    fun updateSpadesBid(playerIndex: Int, bid: Int) {
        val current = _spadesBids.value.toMutableMap()
        current[playerIndex] = bid
        _spadesBids.value = current
    }

    fun updateSpadesTricks(playerIndex: Int, tricks: Int) {
        val current = _spadesTricks.value.toMutableMap()
        current[playerIndex] = maxOf(0, tricks)
        _spadesTricks.value = current
    }

    fun resetSpadesEntry() {
        _spadesBids.value = emptyMap()
        _spadesTricks.value = emptyMap()
    }

    fun submitSpadesRound(gameId: Long) {
        val game = currentGame.value ?: return
        val players = MoshiHelper.fromJsonPlayers(game.playersJson)
        val rounds = MoshiHelper.fromJsonRounds(game.roundsJson)
        val roundNumber = rounds.size + 1

        // Retrieve cumulative bags prior to this round to calculate penalties
        val priorBags = GameRepository.calculateCumulativeBags(players.size, rounds)

        val spadesDetails = players.mapIndexed { index, name ->
            val bid = _spadesBids.value[index] ?: 0
            val tricks = _spadesTricks.value[index] ?: 0
            
            var roundScore = 0
            var bagsGained = 0
            var failedNil = false

            when (bid) {
                -1 -> { // Nil
                    if (tricks == 0) {
                        roundScore = 100
                    } else {
                        roundScore = -100
                        bagsGained = tricks // tricks won on nil act as bags
                        failedNil = true
                    }
                }
                -2 -> { // Blind Nil
                    if (tricks == 0) {
                        roundScore = 200
                    } else {
                        roundScore = -200
                        bagsGained = tricks
                        failedNil = true
                    }
                }
                else -> { // Standard Bids (0..13)
                    if (tricks >= bid) {
                        roundScore = (bid * 10) + (tricks - bid)
                        bagsGained = tricks - bid
                    } else {
                        roundScore = -(bid * 10)
                        bagsGained = 0
                    }
                }
            }

            // Apply Spades 10 bag penalty if cumulative bags cross multiples of 10
            val updatedBagsTotal = priorBags[index] + bagsGained
            var finalRoundScore = roundScore
            
            // Check if bag count crossed a 10-bag boundary:
            // e.g., if (priorBags / 10) < (updatedBags / 10), then subtracting -100 points
            val priorTens = priorBags[index] / 10
            val currentTens = updatedBagsTotal / 10
            if (currentTens > priorTens) {
                finalRoundScore -= 100 
            }

            SpadesPlayerRoundDetail(
                playerName = name,
                bid = bid,
                tricksWon = tricks,
                scoreGained = finalRoundScore,
                bagsGained = bagsGained,
                failedNil = failedNil
            )
        }

        val gameRound = GameRound(
            roundNumber = roundNumber,
            scores = spadesDetails.map { it.scoreGained },
            spadesDetails = spadesDetails
        )

        viewModelScope.launch {
            repository.saveRound(gameId, gameRound)
            resetSpadesEntry()
        }
    }
}
