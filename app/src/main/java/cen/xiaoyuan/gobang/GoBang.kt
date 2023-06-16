package cen.xiaoyuan.gobang

import android.util.Log
import cen.xiaoyuan.gobang.data.Chess
import cen.xiaoyuan.gobang.data.ChessBoard
import cen.xiaoyuan.gobang.data.GameOver
import cen.xiaoyuan.gobang.view.GoBangView
import kotlin.math.min
import kotlin.math.sqrt

class GoBang private constructor(){

    private val gameHelper = GameHelper.instance
    private var gameOverListener:((GameOver?)->Unit)?=null
    private var gameWinnerListener:((Boolean, Chess)->Unit)?=null
    private var gameDoNextListener:((Boolean)->Unit)?=null
    private lateinit var goBangView:GoBangView

    val chessBoard get() = goBangView.chessBoard
    val chessStatus get() = chessBoard.chessStatus
    val allChess get() = chessBoard.allChess
    val isEmpty get() = allChess.isEmpty()

    fun load(data:List<Chess>,block:(()->Unit)?=null){
        clearStatus()
        allChess.addAll(data)
        data.forEach {
            chessStatus[it.x*chessBoard.size+it.y] = it.value
        }
        block?.invoke()
    }

    fun gameOverListener(gameOverBlock:((GameOver?)->Unit),winnerBlock: ((Boolean, Chess)->Unit)){
        gameOverListener = gameOverBlock
        gameWinnerListener = winnerBlock
    }

    fun gameDoNextListener(block:((Boolean)->Unit)){ gameDoNextListener = block }
    fun clearStatus(){ chessBoard.clearStatus() }
    fun play(chess: Chess){ checkStatus(chess, chessBoard) }

    private fun checkStatus(chess: Chess,chessBoard: ChessBoard){
        if(checkStatusTopToDown(chess,chessBoard)
            || checkStatusLeftToRight(chess, chessBoard)
            || checkStatusRightTopToLeftDown(chess, chessBoard)
            || checkStatusLeftTopToRightDown(chess, chessBoard)){
            gameWinnerListener?.invoke(true,chess)
        }else if(chessBoard.enable()) gameDoNextListener?.invoke(!chess.isWhite)
        else gameOverListener?.invoke(null)
    }
    /**
     * 检查 竖直方向 是否有连续5子
     * @param chess 棋子
     * @param chessBoard 棋盘
     */
    private fun checkStatusTopToDown(chess: Chess,chessBoard: ChessBoard):Boolean{
        val sx = chess.x
        val sy = chess.y.leftRange(4)
        val ey = chess.y.rightRange(4,chessBoard.size-1)-4
        val count = ey-sy+1
        repeat(count){
            val y = sy+it
            val result = checkRange(5,chess,chessBoard.size,sx,y,sx,y+4,0,1)
            if(result==5) {
                gameOverListener?.invoke(
                    GameOver(
                        Chess(chess.isWhite,sx+1,y+1),
                        Chess(chess.isWhite,sx+1,y+4+1)
                    )
                )
                return true
            }
        }
        return false
    }
    /**
     * 检查 水平方向 是否有连续5子
     * @param chess 棋子
     * @param chessBoard 棋盘
     */
    private fun checkStatusLeftToRight(chess: Chess,chessBoard: ChessBoard):Boolean{
        val sy = chess.y
        val sx = chess.x.leftRange(4)
        val ex = chess.x.rightRange(4,chessBoard.size-1)
        val offsetX = ex-chess.x
        val count = ex-offsetX-sx+1
        repeat(count){
            val x = sx+it
            val result = checkRange(5,chess,chessBoard.size,x,sy,x+4,sy,1,0)
            if(result==5) {
                gameOverListener?.invoke(
                    GameOver(
                        Chess(chess.isWhite,x+1,sy+1),
                        Chess(chess.isWhite,x+4+1,sy+1)
                    )
                )
                return true
            }
        }
        return false
    }
    /**
     * 检查 左上角到右下角 是否有连续5子
     * @param chess 棋子
     * @param chessBoard 棋盘
     */
    private fun checkStatusLeftTopToRightDown(chess: Chess,chessBoard: ChessBoard):Boolean{
        val sy = chess.y.leftRange(4)
        val sx = chess.x.leftRange(4)
        val offsetXY = min(chess.x-sx,chess.y-sy)
        val count = offsetXY+1
        repeat(count){
            val x = chess.x-offsetXY+it
            val y = chess.y-offsetXY+it
            val result = checkRange(5,chess,chessBoard.size,x,y,x+4,y+4,1,1)
            if(result==5) {
                gameOverListener?.invoke(
                    GameOver(
                        Chess(chess.isWhite,x+1,y+1),
                        Chess(chess.isWhite,x+4+1,y+4+1)
                    )
                )
                return true
            }
        }
        return false
    }

    /**
     * 检查 右上角到左下角 是否有连续5子
     * @param chess 棋子
     * @param chessBoard 棋盘
     */
    private fun checkStatusRightTopToLeftDown(chess: Chess,chessBoard: ChessBoard):Boolean{
        val sy = chess.y.leftRange(4)
        val sx = chess.x.rightRange(4,chessBoard.size-1)
        val offsetXY = min(sx-chess.x,chess.y-sy)
        val count = offsetXY+1
        repeat(count){
            val x = chess.x+offsetXY-it
            val y = chess.y-offsetXY+it
            val result = checkRange(5,chess,chessBoard.size,x,y,x-4,y+4,-1,1)
            if(result==5) {
                gameOverListener?.invoke(
                    GameOver(
                        Chess(chess.isWhite,x+1,y+1),
                        Chess(chess.isWhite,x-4+1,y+4+1)
                    )
                )
                return true
            }
        }
        return false
    }

    /**
     * 检查 起点到终点 是否有连续5子
     * @param count 检查次数
     * @param chess 棋子
     * @param size 棋盘大小
     * @param offsetX X 轴移动方向
     * @param offsetY Y 轴移动方向
     * @param sx,sy,ex,ey 起始坐标
     */
    private fun checkRange(count:Int,chess: Chess,size:Int,sx:Int,sy:Int,ex:Int,ey:Int,offsetX:Int = 0,offsetY:Int = 0):Int{
        if(count<1 || (sx !in 0 until size)  || (sy !in 0 until size)) return 0
        val array = chessStatus[sx*size+sy]
        val tmp = (array!=0 && array==chess.value).int()
        return if(sx==ex && sy==ey) tmp
        else checkRange(count-1,chess,size,sx+offsetX,sy+offsetY,ex,ey,offsetX,offsetY)+tmp
    }

    private fun Boolean.int() = if(this) 1 else 0
    private fun Int.leftRange(base:Int) = if(this-base<0) 0 else this-base
    private fun Int.rightRange(base:Int,limit: Int) = if(this+base>limit) limit else this+base

    fun bind(view: GoBangView){
        goBangView = view
    }

    private fun bytesToString(chessBoard: ChessBoard){
        val buffer = StringBuilder()
        buffer.append("\n")
        repeat(chessBoard.size){ row ->
            repeat(chessBoard.size){ col ->
                buffer.append(chessStatus[row*chessBoard.size+col])
            }
            buffer.append("\n")
        }
        Log.d(TAG, "checkStatus: $buffer")
    }

    companion object {
        private const val TAG = "GoBang"
        val instance: GoBang by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            GoBang()
        }
    }
}