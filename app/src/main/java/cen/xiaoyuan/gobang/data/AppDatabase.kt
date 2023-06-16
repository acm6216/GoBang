package cen.xiaoyuan.gobang.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GoBangGame::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}