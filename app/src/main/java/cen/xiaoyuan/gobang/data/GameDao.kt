package cen.xiaoyuan.gobang.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Insert
    suspend fun insertGameData(goBangGame: GoBangGame)

    @Delete
    suspend fun deleteGameData(goBangGame: GoBangGame)

    @Query("SELECT * FROM game")
    fun queryGames(): Flow<List<GoBangGame?>>
}