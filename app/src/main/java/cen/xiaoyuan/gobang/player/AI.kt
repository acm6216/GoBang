package cen.xiaoyuan.gobang.player

import cen.xiaoyuan.gobang.GoBang
import cen.xiaoyuan.gobang.data.Chess

class AI(goBang: GoBang) : Player(goBang,Chess.white()) {

    private val scoreMap = IntArray(400)
    private val maxScoreMap = ArrayList<Chess>()
    private var maxValue = 0

    private val offsetX = intArrayOf(-1, -1, 0, 1)
    private val offsetY = intArrayOf(0, -1, -1, -1)

    enum class WhiteChessScore(val score:Int){
        ONE(4),TWO(10),THREE(50),FOUR(10000),FIVE(30000),
        BLOCKED_ONE(2),BLOCKED_TWO(5),BLOCKED_THREE(25),BLOCKED_FOUR(55)
    }
    enum class BlackChessScore(val score:Int){
        ONE(4),TWO(10),THREE(40),FOUR(200),FIVE(20000),
        BLOCKED_ONE(2),BLOCKED_TWO(5),BLOCKED_THREE(30),BLOCKED_FOUR(60)
    }

    override fun next() {
        calculateScore()
        maxValue = -1
        maxScoreMap.clear()
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                val it = scoreMap[col * boardSize + row]
                if (it > maxValue) {
                    maxScoreMap.clear()
                    maxScoreMap.add(Chess(true, col + 1, row + 1))
                    maxValue = it
                } else if (it == maxValue) {
                    maxScoreMap.add(Chess(true, col + 1, row + 1))
                }
            }
        }
        val target = maxScoreMap[random.nextInt(maxScoreMap.size)]
        doNext?.invoke(target)
    }

    private fun IntArray.clear() {
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                this[col * boardSize + row] = 0
            }
        }
    }

    private fun chessCount(
        isResetCount: Boolean,
        chessValue: Int,
        row: Int, col: Int,
        x: Int, y: Int,
        block: (Int, Int) -> Unit
    ): AI {
        if (isResetCount) resetCount()
        for (i in 1..4) {
            val curRow = row + i * y
            val curCol = col + i * x
            if ((curRow in 0 until boardSize) && (curCol in 0 until boardSize)) {
                if (chessStatus[curCol * boardSize + curRow] == chessValue)
                    block(1, 0)
                else if (chessStatus[curCol * boardSize + curRow] == Chess.EMPTY_CHESS) {
                    block(0, 1)
                    break
                } else break
            }
        }
        return this
    }

    private fun targetScore(selfCount: Int, emptyCount: Int, block: (BlackChessScore) -> Unit) {
        if (selfCount == 1) {
            if (emptyCount==1) block(BlackChessScore.BLOCKED_TWO)
            else if(emptyCount==2) block(BlackChessScore.TWO)
        }
        else if (selfCount == 2) {
            //连3
            if (emptyCount == 1) block(BlackChessScore.BLOCKED_THREE) //眠3
            else if (emptyCount == 2) block(BlackChessScore.THREE) //活3
        } else if (selfCount == 3) {
            //连4
            if (emptyCount == 1) block(BlackChessScore.BLOCKED_FOUR) //眠4
            else if (emptyCount == 2) block(BlackChessScore.FOUR) //活4
        } else if (selfCount == 4) block(BlackChessScore.FIVE) //连5
    }

    private fun selfScore(selfCount: Int, emptyCount: Int, block: (WhiteChessScore) -> Unit) {
        if (selfCount == 0){
            if(emptyCount==1) block(WhiteChessScore.BLOCKED_ONE)
            else if(emptyCount==2) block(WhiteChessScore.ONE)
        }
        else if (selfCount == 1) {
            if(emptyCount==1) block(WhiteChessScore.BLOCKED_TWO)
            else if(emptyCount==2) block(WhiteChessScore.TWO)
        }
        else if (selfCount == 2) {
            //连3
            if (emptyCount == 1) block(WhiteChessScore.BLOCKED_THREE) //眠3
            else if (emptyCount == 2) block(WhiteChessScore.THREE) //活3
        } else if (selfCount == 3) {
            //连4
            if (emptyCount == 1) block(WhiteChessScore.BLOCKED_FOUR) //眠4
            else if (emptyCount == 2) block(WhiteChessScore.FOUR) //活4
        } else if (selfCount == 4) block(WhiteChessScore.FIVE)
    }

    private fun foreach(block: (Int, Int) -> Unit) {
        for (i in 0 until 4) {
            block(offsetX[i], offsetY[i])
        }
    }

    private fun doubleForeach(block: (Int, Int) -> Unit) {
        for (row in 0 until boardSize) {
            for (col in 0 until boardSize) {
                if (isNotEmpty(row, col)) continue
                block(row, col)
            }
        }
    }

    private var personNum = 0
    private var aiNum = 0
    private var emptyNum = 0

    private fun resetCount() {
        personNum = 0
        aiNum = 0
        emptyNum = 0
    }

    private fun calculateScore() {
        scoreMap.clear()
        doubleForeach { row, col ->
            foreach { x, y -> //4个方向搜索
                chessCount(true, chessType.opponent(), row, col, x, y) { person, empty ->
                    //对手在此落子会构成什么棋形
                    personNum += person
                    emptyNum += empty
                }.chessCount(false, chessType.opponent(), row, col, -x, -y) { person, empty ->
                    //计算反方向
                    personNum += person
                    emptyNum += empty
                }.targetScore(personNum, emptyNum) {
                    //计算对手得分
                    scoreMap[col * boardSize + row] = it.score
                }

                chessCount(true, chessType.value, row, col, x, y) { ai, empty ->
                    //自己在此落子形成什么棋形
                    aiNum += ai
                    emptyNum += empty
                }.chessCount(false, chessType.value, row, col, -x, -y) { ai, empty ->
                    //计算反方向
                    aiNum += ai
                    emptyNum += empty
                }.selfScore(aiNum, emptyNum) {
                    //计算自己得分
                    scoreMap[col * boardSize + row] += it.score
                }
            }
        }
    }

    companion object {
        private const val TAG = "AI"
    }

}