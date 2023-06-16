package cen.xiaoyuan.gobang

class GameHelper private constructor() {

    external fun pingByJNI(byteArray: ByteArray, chessBoard: Int)

    companion object {
        init {
            System.loadLibrary("game")
        }

        val instance: GameHelper by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            GameHelper()
        }
    }
}