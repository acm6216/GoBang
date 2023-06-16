package cen.xiaoyuan.gobang.data

sealed class ChessBoard(val id:Int){

    class ChessBoard15:ChessBoard(15+1)
    class ChessBoard17:ChessBoard(17+1)
    class ChessBoard19:ChessBoard(19+1)

    val size = id-1
    val chessStatus = IntArray(400)
    val allChess = ArrayList<Chess>()

    fun enable() = allChess.size != size*size

    fun clearStatus() {
        allChess.clear()
        for (index in chessStatus.indices) {
            chessStatus[index] = 0
        }
    }

}