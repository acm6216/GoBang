package cen.xiaoyuan.gobang.adapters

import android.annotation.SuppressLint
import android.text.format.DateFormat
import android.widget.ProgressBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import cen.xiaoyuan.gobang.data.GoBangGame
import cen.xiaoyuan.gobang.treeObserver
import cen.xiaoyuan.gobang.view.GoBangMiniView
import com.google.gson.Gson
import java.util.*

@SuppressLint("SetTextI18n")
@BindingAdapter("board_size")
fun TextView.boardSize(size:Int){
    text = "$size"
}

@BindingAdapter("game_date")
fun TextView.gameDate(date:Long){
    text = date.dateToString()
}

@BindingAdapter("game_date")
fun TextView.gameDate(date:String) = gameDate(date.toLong())

fun Long.dateToString(pattern: String? = "yy/MM/dd-hh:mm:ss"): String {
    val date = Date(this)
    return DateFormat.format(pattern, date.time).toString()
}

@BindingAdapter("game_progress","game_max_value", requireAll = true)
fun ProgressBar.gameProgress(progressValue:Int,size:Int){
    max = size*size
    progress = progressValue
}

@BindingAdapter("game_data","game_gson", requireAll = true)
fun GoBangMiniView.showGameData(game: GoBangGame,gson: Gson){
    treeObserver {
        setData(game,gson)
    }
}