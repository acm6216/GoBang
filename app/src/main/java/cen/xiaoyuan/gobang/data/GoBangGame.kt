package cen.xiaoyuan.gobang.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game")
data class GoBangGame(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val content:String,
    val title:String,
    val date:Long,
    val time:Long = 0,
    val boardSize:Int = 15,
    val progress:Int = 0,
    val type:Int = 0
)