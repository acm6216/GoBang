package cen.xiaoyuan.gobang.player

import android.util.Log
import cen.xiaoyuan.gobang.GoBang
import cen.xiaoyuan.gobang.data.Chess
import kotlin.random.Random

abstract class Player(val goBang: GoBang,val chessType:Chess) {

    protected val chessStatus get() = goBang.chessStatus
    protected val boardSize get() = goBang.chessBoard.size
    protected val chessCount get() = goBang.allChess.size
    protected val random = Random.Default

    protected fun isNotEmpty(row:Int, col:Int):Boolean = chessStatus[col*boardSize+row]!=0

    open fun play(chess: Chess){
        chessStatus[chess.x*boardSize+chess.y] = chess.value
    }

    protected var doNext:((Chess)->Unit)?=null
    fun doNext(block:((Chess)->Unit)){
        doNext = block
    }

    abstract fun next()

    companion object{
        private const val TAG = "Player"
    }

}