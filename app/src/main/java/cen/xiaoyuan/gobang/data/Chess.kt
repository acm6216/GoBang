package cen.xiaoyuan.gobang.data

data class Chess(val isWhite:Boolean,val col:Int,val row:Int){
    val value:Int = if (isWhite) WHITE_CHESS else BLACK_CHESS
    val x get() = col - 1
    val y get() = row - 1
    val name get() = if(isWhite) "白棋" else "黑棋"
    companion object{
        const val WHITE_CHESS = 2
        const val BLACK_CHESS = 1
        const val EMPTY_CHESS = 0
        fun white(col:Int = 0,row:Int = 0):Chess = Chess(true,col,row)
        fun black(col:Int = 0,row:Int = 0):Chess = Chess(false,col,row)
        fun fromValue(value:Int,col:Int,row:Int) = Chess(value== WHITE_CHESS,col,row)
    }
    fun opponent():Int = if(value== WHITE_CHESS) BLACK_CHESS else WHITE_CHESS
}