package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class GameType {
    HEARTS,
    SPADES
}

enum class GameStatus {
    ACTIVE,
    COMPLETED
}

@JsonClass(generateAdapter = true)
data class SpadesPlayerRoundDetail(
    val playerName: String,
    val bid: Int,          // -1 for Nil, -2 for Blind Nil, or 0..13 for standard bids
    val tricksWon: Int,    // 0..13 actual tricks won
    val scoreGained: Int,  // calculated points for this round
    val bagsGained: Int,   // calculated overtricks (bags) for this round
    val failedNil: Boolean = false
)

@JsonClass(generateAdapter = true)
data class HeartsPlayerRoundDetail(
    val playerName: String,
    val scoreGained: Int,  // points accumulated, e.g., 1 pt per Heart, 13 pts for Queen of Spades
    val shotTheMoon: Boolean = false,
    val queenOfSpadesTaken: Boolean = false,
    val heartsTaken: Int = 0
)

@JsonClass(generateAdapter = true)
data class GameRound(
    val roundNumber: Int,
    val scores: List<Int>, // total scores accumulated in this round for each player (matching the indexing of players)
    val spadesDetails: List<SpadesPlayerRoundDetail>? = null,
    val heartsDetails: List<HeartsPlayerRoundDetail>? = null
)

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameType: String, // "HEARTS" or "SPADES"
    val playersJson: String, // List of Player Names in JSON
    val status: String, // "ACTIVE" or "COMPLETED"
    val targetScore: Int, // e.g. 50, 100, 250, 500
    val winnerName: String? = null,
    val roundsJson: String, // List of GameRound in JSON
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "player_profiles")
data class PlayerProfile(
    @PrimaryKey val name: String,
    val colorHex: String, // Hex string represent color e.g., #EF4444
    val avatarEmoji: String, // Emoji string represent avatar e.g., 🤠
    val createdAt: Long = System.currentTimeMillis()
)
