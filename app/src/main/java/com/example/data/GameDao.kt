package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE status = 'ACTIVE' LIMIT 1")
    fun getActiveGameSymbolic(): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    fun getGameById(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameByIdOneShot(id: Long): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()

    // --- Player Profile Methods ---

    @Query("SELECT * FROM player_profiles ORDER BY name ASC")
    fun getAllPlayerProfiles(): Flow<List<PlayerProfile>>

    @Query("SELECT * FROM player_profiles WHERE name = :name LIMIT 1")
    suspend fun getPlayerProfileByName(name: String): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerProfile(profile: PlayerProfile)

    @Update
    suspend fun updatePlayerProfile(profile: PlayerProfile)

    @Query("DELETE FROM player_profiles WHERE name = :name")
    suspend fun deletePlayerProfile(name: String)
}
