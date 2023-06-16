package cen.xiaoyuan.gobang.player

import android.util.Log
import cen.xiaoyuan.gobang.GoBang
import cen.xiaoyuan.gobang.data.Chess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * https://github.com/anlingyi/
 * xechat-idea/blob/main/
 * xechat-plugin/src/main/
 * java/cn/xeblog/plugin/
 * game/gobang/ZhiZhangAIService.java
 */

class AI2(
    goBang: GoBang,
    private val aiConfig: AiConfig = AiConfig(6, 10, 2, 6)
):Player(goBang, Chess.white()),CoroutineScope {

    override val coroutineContext: CoroutineContext get() = Dispatchers.IO
    data class Point(val x:Int,val y:Int,var type:Int){
        var score:Int = 0
        override fun toString(): String = "${if(type == 1) "黑" else "白" }[$x,$y]"
    }
    data class AiConfig(val depth:Int,val maxNodes:Int,val vcx:Int,val vcxDepth:Int)
    data class SituationCache(val depth:Int,val point: Point?=null,val score:Int = 0)
    data class Statistics(
        var depth:Int = 0, var point:Point? = null, var score:Int = 0,
        var minimaxTime:Double = 0.0, var nodes:Int = 0, var cuts:Int = 0,
        var vcxDepth:Int = 0, var vcx:Int = 0, var vcxTime:Double = 0.0,
        var caches:Int = 0, var cacheHits:Int = 0
    )

    private var attack:Float = if(chessType.isWhite) 1.0f else 1.8f
    private var bestPoint:Point?=null
    private var rounds:Int = 0
    private var statistics:Statistics? = null
    private var hashcode: Long = 0
    private var situationCacheMap: MutableMap<Long, SituationCache>? = null

    private var ai:Int = 2
    private var isFinish = true

    private val infinity = 999999999
    private val scoreMap: MutableMap<String, Int> = LinkedHashMap()

    private val chessData = Array(20){IntArray(20)}

    /**
     * 黑子 Zobrist 转置表
     */
    private val blackZobrist = Array(20) { LongArray(20) }

    /**
     * 白子 Zobrist 转置表
     */
    private val whiteZobrist = Array(20) { LongArray(20) }

    init {
        // 初始化棋型分数表
        for (chessScore in ChessModel.values()) {
            for (value in chessScore.values)
                scoreMap[value] = chessScore.score
        }

        // 初始化Zobrist随机值
        foreach(blackZobrist.indices){ i,j ->
            blackZobrist[i][j] = random.nextLong()
            whiteZobrist[i][j] = random.nextLong()
        }
    }

    private fun foreach(value:IntRange,block:((Int,Int)->Unit)) {
        for (row in value)
            for (col in value) block(row,col)
    }

    private fun foreach(value:Int,block:((Int,Int)->Unit)) {
        for (row in 0 until value)
            for (col in 0 until value) block(row,col)
    }

    /**
     * @param score 分数
     * @param values 局势数组
     */
    private enum class ChessModel(val score: Int, val values: Array<String>) {
        LIANWU(10000000, arrayOf("11111")),
        HUOSI(1000000, arrayOf("011110")),
        HUOSAN(10000, arrayOf("001110", "011100", "010110", "011010")),
        CHONGSI(9000, arrayOf("11110", "01111", "10111", "11011", "11101")),
        HUOER(100, arrayOf("001100", "011000", "000110", "001010", "010100")),
        HUOYI(80, arrayOf("010200", "002010", "020100", "001020", "201000", "000102", "000201")),
        MIANSAN(30, arrayOf("001112", "010112", "011012", "211100", "211010")),
        MIANER(10, arrayOf("011200", "001120", "002110", "021100", "110000", "000011", "000112", "211000")),
        MIANYI(1, arrayOf("001200", "002100", "000210", "000120", "210000", "000012"));
    }

    /**
     * 风险评分
     * @param score 分数
     */
    private enum class RiskScore(var score: Int) {
        HIGH_RISK(800000),
        MEDIUM_RISK(500000),
        LOW_RISK(100000);

        companion object {
            /**
             * 判断分数是否处于区间 [leftScore, rightScore)
             *
             * @param score      分数
             * @param leftScore  左区分值
             * @param rightScore 右区分值
             * @return
             */
            fun between(score: Int, leftScore: RiskScore, rightScore: RiskScore): Boolean {
                return score >= leftScore.score && score < rightScore.score
            }
        }
    }

    override fun next() {
        launch {
            isFinish = false
            go()?.let {
                if(!isFinish)
                doNext?.invoke(
                    Chess(it.type == Chess.WHITE_CHESS, it.y + 1, it.x + 1)
                )
                isFinish = true
            }
        }
    }

    fun thinking() = !isFinish

    fun finish(){
        isFinish = true
    }

    private fun go():Point?{
        initChessStatus()
        statistics = Statistics()
        bestPoint = null
        ai = chessType.value
        var depth: Int = aiConfig.depth
        if (rounds == 1 && !chessType.isWhite) {
            // AI先下，首子天元
            val centerX: Int = boardSize / 2
            val centerY: Int = boardSize / 2
            return Point(centerX, centerY, ai)
        }
        // 基于普通方式获取最佳棋位
        if (aiConfig.depth < 2) {
            return getBestPoint()
        }
        if (aiConfig.depth > 4 && this.rounds < 4) {
            // 当AI级别大于4时，将前三个回合的搜索深度设置为4
            depth = 4
        }

        // 算杀模式
        val vcx = aiConfig.vcx
        if (vcx > 0) {
            // 算杀最大深度
            val vcxDepth = aiConfig.vcxDepth
            val vcxStartTime = System.currentTimeMillis()
            // VCT/VCF
            bestPoint = deepeningVcx(true, vcxDepth, vcx == 2)
            val vcxEndTime = System.currentTimeMillis()
            statistics!!.vcxTime = (vcxEndTime - vcxStartTime) / 1000.00
        }

        if (bestPoint == null) {
            statistics!!.nodes = 0
            statistics!!.cacheHits = 0
            val minimaxStartTime = System.currentTimeMillis()
            // 基于极大极小值搜索获取最佳棋位
            bestPoint = deepeningMinimax(2, depth)
            val minimaxEndTime = System.currentTimeMillis()
            statistics!!.minimaxTime = (minimaxEndTime - minimaxStartTime) / 1000.0
        }

        statistics!!.point = this.bestPoint
        bestPoint?.let {
            statistics!!.score = it.score
        }
        situationCacheMap?.let {
            statistics!!.caches = it.size
        }

        this.situationCacheMap = null
        return bestPoint!!
    }

    companion object{
        private const val TAG = "AI2"
    }

    private fun initChessStatus(){
        foreach(boardSize){row,col ->
            chessData[row][col] = chessStatus[col*boardSize+row]
        }
        rounds = chessCount/2+1
    }

    private fun getBestPoint():Point?{
        if(isFinish) return null
        var best: Point? = null
        // 初始分值为最小
        var score = -infinity
        /* 遍历所有能下棋的点位，评估各个点位的分值，选择分值最大的点位 */
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                if (this.chessData[i][j] != 0) {
                    // 该点已有棋子，跳过
                    continue
                }

                val p = Point(i, j, ai)
                // 该点得分 = AI落子得分 * 进攻系数 + 对手落子得分
                val value = (evaluate(p) * this.attack).roundToInt() + evaluate(Point(i, j, 3-ai))
                // 选择得分最高的点位
                if (value > score) {
                    // 最高分被刷新
                    score = value
                    // 更新最佳点位
                    best = p
                }
            }
        }
        return best!!
    }

    private fun evaluate(point: Point):Int{
        // 分值
        var score = 0
        // 活三数
        var huosanTotal = 0
        // 冲四数
        var chongsiTotal = 0
        // 统计同一方向既冲四又活三的情况，出现这种情况的优先按活三分计算
        var tfTotal = 0
        for (i in 1 until 5) {
            // 获取当前局势
            val situation = getSituation(point, i)
            // 获取当前局势的棋型
            val chessModel:ChessModel? = getChessModel(situation)

            // 棋型统计
            if (chessModel != null) {
                when (chessModel) {
                    ChessModel.HUOSAN -> {
                        // 活三+1
                        huosanTotal++
                        if (checkSituation(situation, ChessModel.CHONGSI)) {
                            // 同一方向出现活三，又出现冲四
                            tfTotal++
                        }
                    }
                    ChessModel.CHONGSI ->
                    // 冲四+1
                    chongsiTotal++
                    else -> {}
                }
                // 下此步的得分
                score += chessModel.score
            }
        }

        if (chongsiTotal > 1 || tfTotal > 1) {
            // 冲四数大于1，+高风险评分
            score += RiskScore.HIGH_RISK.score
        } else if (chongsiTotal > 0 && huosanTotal > 0 || tfTotal > 0 && huosanTotal > 1) {
            // 冲四又活三，+中风险评分
            score += RiskScore.MEDIUM_RISK.score
        } else if (huosanTotal > 1) {
            // 活三数大于1，+低风险评分
            score += RiskScore.LOW_RISK.score
        }

        point.score = score
        return score

    }

    /**
     * 迭代加深VCX
     *
     * @param isAi     是否是AI
     * @param maxDepth 最大深度
     * @param isVcf    true:VCF false:VCT
     * @return
     */
    private fun deepeningVcx(isAi:Boolean, maxDepth:Int, isVcf:Boolean):Point? {
        ai = if(isAi) ai else 3 - ai
        val point:Point? = deepening(1, maxDepth, isVcf)
        if (!isAi) {
            ai = 3 - ai
            if (point != null) {
                point.type = ai
            }
        }

        return point
    }

    /**
     * 迭代加深minimax搜索
     *
     * @param depth    当前搜索深度
     * @param maxDepth 最大搜索深度
     * @return
     */
    private fun deepeningMinimax(depth:Int, maxDepth:Int):Point? {
        situationCacheMap = HashMap<Long,SituationCache>(2048)

        var best:Point? = null
        for (depthValue in depth..maxDepth step 2) {
            val score:Int = minimax(0, depthValue, -infinity, infinity)
            best = this.bestPoint
            statistics?.point = best
            statistics?.depth = depthValue
            statistics?.score = score

            if (abs(score) >= infinity - 1) {
                // 找到最优解了，结束搜索
                break
            }
        }

        return best
    }

    /**
     * 启发式获取落子点位
     *
     * @param type 当前走棋方 1.黑棋 2.白棋
     * @return
     */
    private fun getHeuristicPoints(type:Int):List<Point> {
        // 落子点上限
        val max = aiConfig.maxNodes
        // 高优先级落子点
        val highPriorityPointList = ArrayList<Point>()
        // 低优先级落子点
        val lowPriorityPointList = ArrayList<Point>()
        // 候补落子点
        val alternatePointList = ArrayList<Point>()
        // 杀棋点
        val killPointList = ArrayList<Point>()

        // 局势危险等级 0.不危险 1.有危险 2.很危险
        var dangerLevel = 0
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
            if (this.chessData[i][j] != 0) {
                // 该处已有棋子，跳过
                continue
            }

            // 考虑自己的落子情况
            val point = Point(i, j, type)
            val score = evaluate(point)
            if (score >= ChessModel.LIANWU.score) {
                // 优先检查自己连五的情况，如果该落子点可以形成连五，则结束循环，直接返回
                return point.list()
            }

            if (dangerLevel == 2) {
                // 局势很危险，只检查自己可以连五的落子点
                continue
            }

            if (score >= RiskScore.MEDIUM_RISK.score) {
                // 必杀棋
                killPointList.add(point)
            }

            // 考虑对手的落子情况
            val foePoint = Point(i, j, 3 - type)
            val foeScore = evaluate(foePoint)
            // 当前局势危险等级
            var level = 0
            if (foeScore >= ChessModel.LIANWU.score) {
                // 对手连五了，局势很危险！！
                level = 2
            } else if (foeScore >= RiskScore.MEDIUM_RISK.score) {
                // 对手有活四、双冲四、冲四活三的点位了，局势有危险！
                level = 1
            }

            if (level > 0) {
                // 当前局势存在危险
                if (dangerLevel < level) {
                    // 危险升级
                    dangerLevel = level
                    // 局势危险等级如果上升，则清空之前选择的高优先级节点，防止AI误入歧途
                    highPriorityPointList.clear()
                }

                // 将此节点加入到高优先级队列
                highPriorityPointList.add(point)
            }

            if (dangerLevel > 0) {
                // 局势有危险，下面的检查逻辑不用走了
                continue
            }

            if (RiskScore.between(score, RiskScore.LOW_RISK, RiskScore.MEDIUM_RISK)
                || RiskScore.between(foeScore, RiskScore.LOW_RISK, RiskScore.MEDIUM_RISK)) {
                // 高优先级落子点：多活三，需考虑对手
                highPriorityPointList.add(point)
                continue
            }

            if (highPriorityPointList.isEmpty()) {
                if (score >= ChessModel.CHONGSI.score || foeScore >= ChessModel.CHONGSI.score) {
                    // 低优先级落子点：冲四、活三，需考虑对手
                    lowPriorityPointList.add(point)
                    continue
                }

                if (lowPriorityPointList.isEmpty() && score >= ChessModel.MIANYI.score) {
                    // 候补落子点：活二、活一、眠三、眠二、眠一，不用考虑对手
                    alternatePointList.add(point)
                }
            }
        }
        }

        if (dangerLevel < 2 && killPointList.isNotEmpty()) {
            // 局势不是特别危险，且杀棋队列不为空，直接进攻就好
            return killPointList
        }

        val pointList:List<Point> = if (highPriorityPointList.isEmpty()) {
            // 无高优先级落子点，则判断是否有低优先级落子点
            if (lowPriorityPointList.isEmpty()) {
                // 低优先级落子点也没有，就返回候补落子点
                if (alternatePointList.isEmpty()) {
                    // 候补落子点也没有，就随机取
                    return randomPoint(type, 1)
                }

                // 打乱一下
                alternatePointList.shuffle()
                // 返回打乱后的候补落子点
                alternatePointList
            } else {
                // 返回低优先级落子点
                lowPriorityPointList
            }
        } else {
            // 返回高优先级落子点
            highPriorityPointList
        }

        // 按分数从大到小排序
        pointList.sortedWith{p1,p2 ->
            if (p1.score == p2.score) 0
            else if (p1.score > p2.score) -1
            else 1
        }

        // 取最大节点个数
        return pointList.subList(0, pointList.size.coerceAtMost(max))
    }

    /**
     * 随机获取落子点
     *
     * @param type 棋子类型
     * @param num  数量
     * @return
     */
    private fun randomPoint(type:Int, num:Int):List<Point> {
        val pointList = ArrayList<Point>()
        foreach(boardSize){ i,j->
            if (this.chessData[i][j] == 0) {
                pointList.add(Point(i, j, type))
            }
        }
        pointList.shuffle()
        return pointList.subList(0, num.coerceAtMost(pointList.size))
    }

    /**
     * 极大极小值搜索、AlphaBeta剪枝
     *
     * @param type  当前走棋方 0.根节点表示AI走棋 1.AI 2.玩家
     * @param depth 搜索深度
     * @param alpha 极大值
     * @param beta  极小值
     * @return
     */
    private fun minimax(type:Int, depth:Int, alpha:Int, beta:Int):Int {
        var typeValue = type
        var alphaValue = alpha
        var betaValue = beta
        // 是否是根节点
        val isRoot = typeValue == 0
        if (isRoot) {
            // 根节点是AI走棋
            typeValue = ai
        }

        // 当前是否是AI走棋
        val isAI = typeValue == ai

        // 先查看当前局面是否存在缓存
        val situationCache = situationCacheMap?.get(this.hashcode)
        if (situationCache != null && situationCache.depth >= depth) {
            /*if (this.aiConfig.isDebug()) {
                this.statistics.incrCacheHits();
            }*/

            // 缓存存在，且记录的搜索深度比当前深度要深或相等，则直接返回记录的分值
            return situationCache.score
        }

        // 到达叶子结点
        if (depth == 0) {
            /**
             * 评估每棵博弈树的叶子结点的局势
             * 比如：depth=2时，表示从AI开始走两步棋之后的局势评估，AI(走第一步) -> 玩家(走第二步)，然后对局势进行评估
             * 注意：局势评估是以AI角度进行的，分值越大对AI越有利，对玩家越不利
             */
            return evaluateAll()
        }

        // 启发式搜索
        val pointList:List<Point> = getHeuristicPoints(typeValue)
        if (isRoot && pointList.size == 1) {
            /*if (this.aiConfig.isDebug()) {
                this.statistics.incrNodes();
            }*/

            // 只有一个落子点，直接返回就好
            bestPoint = pointList[0]
            return bestPoint!!.score
        }

        // 记录选择的最好落子点
        val bestPointList = ArrayList<Point>()
        for (point in pointList) {
            /*if (this.aiConfig.isDebug()) {
                this.statistics.incrNodes();
            }*/

            if (point.score >= ChessModel.LIANWU.score) {
                // 落子到这里就赢了，如果是AI落的子就返回最高分，否则返回最低分
                point.score = if(isAI) infinity - 1 else -infinity + 1
            } else {
                /* 模拟 AI -> 玩家 交替落子 */
                // 落子
                putChess(point)
                // 递归生成博弈树，并评估叶子结点的局势
                point.score = minimax(3 - typeValue, depth - 1, alphaValue, betaValue)
                // 撤销落子
                revokeChess(point)
            }

            if (isAI) {
                // AI要选对自己最有利的节点（分最高的）
                if (point.score >= alphaValue) {
                    if (isRoot) {
                        if (point.score > alphaValue) {
                            // 找到了更好的落子点，将之前选择的落子点清空
                            bestPointList.clear()
                        }
                        // 记录该落子点
                        bestPointList.add(point)
                    }

                    // 最高值被刷新，更新alpha值
                    alphaValue = point.score
                }
            } else {
                // 对手要选对AI最不利的节点（分最低的）
                if (point.score < betaValue) {
                    // 最低值被刷新，更新beta值
                    betaValue = point.score
                }
            }

            if (alphaValue >= betaValue) {
                /*if (this.aiConfig.isDebug()) {
                    this.statistics.incrCuts();
                }*/

                /*
                 AlphaBeta剪枝
                 解释：
                 AI当前最大分数为：alpha 搜索区间 (alpha, +∞]
                 对手当前最小分数为：beta 搜索区间 [-∞, beta)
                 因为对手要选择分数小于beta的分支，AI要从对手给的分支里面选最大的分支，这个最大的分支要和当前的分支(alpha)做比较，
                 现在alpha都比beta大了，下面搜索给出的分支也都是小于alpha的，所以搜索下去没有意义，剪掉提高搜索效率。
                 */
                break
            }
        }

        if (isRoot) {
            // 如果有多个落子点，则通过getBestPoint方法选择一个最好的
            if (bestPointList.isNotEmpty())
                this.bestPoint = if(bestPointList.size > 1) getBestPoint(bestPointList) else bestPointList[0]
        }

        val score = if(isAI) alphaValue else betaValue
        // 缓存当前局面分值
        situationCacheMap?.put(this.hashcode, SituationCache(depth,null,score))

        return score
    }

    /**
     * 以AI角度对当前局势进行评估，分数越大对AI越有利
     *
     * @return
     */
    private fun evaluateAll():Int {
        // AI得分
        var aiScore = 0
        // 对手得分
        var foeScore = 0

        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
            val type = this.chessData[i][j]
            if (type == 0) {
                // 该点没有棋子，跳过
                continue
            }

            // 评估该棋位分值
            val value = evaluate(Point(i, j, type))
            if (type == this.ai) {
                // 累积AI得分
                aiScore += value
            } else {
                // 累积对手得分
                foeScore += value
            }
        }
        }

        // 该局AI最终得分 = AI得分 * 进攻系数 - 对手得分
        return (aiScore * this.attack).roundToInt() - foeScore
    }

    /**
     * 从给定的点位列表中获取最佳点位
     *
     * @param pointList 点位列表
     * @return
     */
    private fun getBestPoint(pointList:List<Point>):Point? {
        var bestPoint:Point? = null
        var bestScore = -infinity

        for (i in pointList.indices) {
            val point = pointList[i]
            val score = (evaluate(point) * this.attack).roundToInt() + evaluate(Point(point.x, point.y, 3 - point.type))
            if (score > bestScore) {
                bestScore = score
                bestPoint = point
            }
        }

        return bestPoint
    }

    /**
     * 迭代加深算杀搜索
     *
     * @param depth    当前搜索深度
     * @param maxDepth 最大搜索深度
     * @param isVcf    true:VCF false:VCT
     * @return
     */
    private fun deepening(depth:Int, maxDepth:Int, isVcf:Boolean):Point? {
        situationCacheMap = HashMap<Long,SituationCache>(2048)
        var point:Point? = null
        for (depthValue in depth..maxDepth) {
            this.statistics?.vcxDepth =depthValue
            /*if (this.aiConfig.isDebug()) {
                this.pathStack = Stack<>();
            }*/

            if(isFinish) return null

            point = vcx(0, depthValue, isVcf)
            if (point != null) {
                /*if (this.aiConfig.isDebug()) {
                    StringBuilder pathOut = new StringBuilder();
                    pathOut.append(isVcf ? "VCF" : "VCT").append("路径：");
                    for (Point p : bestPathStack){
                        pathOut.append(p).append(" ");
                    }
                    //this.bestPathStack.forEach(p -> pathOut.append(p).append(" "));
                    //ConsoleAction.showSimpleMsg(pathOut.toString());
                }*/

                // 算杀成功
                statistics?.vcx = if(isVcf) 1 else 2
                break
            }
        }

        return point
    }

    /**
     * 算杀（VCF、VCT）
     *
     * @param type  当前走棋方 1.AI 2.玩家
     * @param depth 搜索深度
     * @param isVcf true:VCF false:VCT
     * @return
     */
    private fun vcx(type:Int, depth:Int, isVcf:Boolean):Point? {
        var typeValue = type
        // 先查看当前局面是否存在缓存
        val situationCache = situationCacheMap!![hashcode]
        if (situationCache != null && situationCache.depth >= depth) {
            // 缓存存在，且记录的搜索深度比当前深度要深或相等，则直接返回记录的局面点位
            return situationCache.point
        }

        if (depth == 0) {
            // 算杀失败
            return null
        }

        val isRoot = typeValue == 0
        if (isRoot) {
            typeValue = ai
        }
        val isAI = typeValue == ai

        var best:Point? = null
        val pointList:List<Point> = getVcxPoints(typeValue, isVcf)
        for (point in pointList) {
            /*if (this.aiConfig.isDebug()) {
                this.statistics.incrNodes();
                this.pathStack.push(point);
            }*/

            if(isFinish) return null

            if (point.score >= RiskScore.HIGH_RISK.score) {
                /*if (this.aiConfig.isDebug()) {
                    if (isAI) {
                        this.bestPathStack = (Stack<Point>) this.pathStack.clone();
                    }
                    this.pathStack.pop();
                }*/

                // 已经可以形成必胜棋型了，如果是AI落子，就返回当前节点，否则返回空
                return if(isAI) point else null
            }

            putChess(point)
            best = vcx(3 - typeValue, depth - 1, isVcf)
            revokeChess(point)

            /*if (this.aiConfig.isDebug()) {
                this.pathStack.pop();
            }*/

            if (best == null) {
                if (isAI) {
                    // AI还没找到可以算杀成功的棋子，继续找...
                    continue
                }

                // 对手拦截成功了，直接返回空出去，表示算杀失败了
                return null
            }

            // 记录当前节点
            best = point

            if (isAI) {
                // AI已经找到可以算杀的棋子了，同层后续的节点都不用看了
                break
            }
        }

        // 缓存当前局面点位
        if(best!=null)
        situationCacheMap!![this.hashcode] = SituationCache(depth,best)

        return best
    }

    /**
     * 获取算杀落子点
     *
     * @param type  当前走棋方 0.根节点表示AI走棋 1.黑棋 2.白棋
     * @param isVcf 是否是连续冲四
     * @return
     */
    private fun getVcxPoints(type:Int, isVcf:Boolean):List<Point> {
        val isAI = type == ai
        // 进攻点列表
        val attackPointList = ArrayList<Point>()
        // 防守点列表
        val defensePointList = ArrayList<Point>()
        // VCX列表
        val vcxPointList = ArrayList<Point>()

        // 局势是否危险
        var isDanger = false
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
            if (this.chessData[i][j] != 0) {
                // 该处已有棋子，跳过
                continue
            }

            // 考虑自己的落子情况
            val point = Point(i, j, type)
            val score = evaluate(point)
            if (score >= ChessModel.LIANWU.score) {
                // 自己可以连五，直接返回
                return point.list()
            }

            if (isDanger) {
                // 存在危险，继续找自己可以连五的棋子
                continue
            }

            // 考虑对手的落子情况
            val foePoint = Point(i, j, 3 - type)
            val foeScore = evaluate(foePoint)
            if (foeScore >= ChessModel.LIANWU.score) {
                // 对手连五了，局势很危险！！赶紧找自己可以连五的点位，不行就防守
                isDanger = true
                defensePointList.clear()
                defensePointList.add(point)
                continue
            }

            // 看看自己有没有大于等于中风险分值的点位
            if (score >= RiskScore.MEDIUM_RISK.score) {
                attackPointList.add(point)
                continue
            }

            if (isAI) {
                // AI才进行VCX
                if (checkSituation(point, ChessModel.CHONGSI)) {
                    // 不论是VCF还是VCT，AI都可先选择冲四
                    vcxPointList.add(point)
                } else if (!isVcf && checkSituation(point, ChessModel.HUOSAN)) {
                    // 记录VCT点位
                    vcxPointList.add(point)
                }
            } else {
                // 对手需防守VCT
                if (!isVcf) {
                    if (checkSituation(point, ChessModel.CHONGSI) || foeScore >= ChessModel.HUOSI.score) {
                        // 选择冲四或防守对方的活四
                        defensePointList.add(point)
                    }
                }
            }
        }
        }

        val pointList = ArrayList<Point>()
        // 没风险就进攻
        if (!isDanger) {
            // 优先强进攻
            if (attackPointList.isNotEmpty()) {
                // 按分数从大到小排序
                attackPointList.sortWith{ p1, p2 ->
                    if (p1.score == p2.score) 0
                    else if (p1.score > p2.score) -1
                    else 1
                }

                if (isAI) {
                    // AI有强进攻点位了，就不用考虑后面的点位了
                    return attackPointList
                }

                // 对手可以选择进攻和防守
                pointList.addAll(attackPointList)
            }

            // VCX进攻
            if (vcxPointList.isNotEmpty()) {
                pointList.addAll(vcxPointList)
            }
        }

        if (defensePointList.isNotEmpty()) {
            // 进行防守
            if (isAI) {
                // AI优先选择进攻，把防守点位放后面
                pointList.addAll(defensePointList)
            } else {
                // 对手优先考虑防守，把防守点位放前面
                pointList.addAll(0, defensePointList)
            }
        }

        return pointList
    }

    /**
     * 检查当前落子是否处于某一局势
     *
     * @param point       当前棋位
     * @param chessModels 检查的局势
     * @return
     */
    private fun checkSituation(point:Point, vararg chessModels:ChessModel):Boolean {
        // 要检查4个大方向
        for (i in 1 until 5) {
            val situation = getSituation(point, i)
            for (chessModel in chessModels) {
                if (checkSituation(situation, chessModel)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 下棋子
     *
     * @param point 棋子
     */
    private fun putChess(point:Point) {
        chessData[point.x][point.y] = point.type
        calculateHashCode(point)
    }

    /**
     * 撤销下的棋子
     *
     * @param point 棋子
     */
    private fun revokeChess(point:Point) {
        chessData[point.x][point.y] = 0
        // 计算当前局面的hashcode
        calculateHashCode(point)
    }

    /**
     * 计算当前落子局面的hashcode
     *
     * @param point
     * @return
     */
    private fun calculateHashCode(point:Point):Long {
        val x = point.x
        val y = point.y
        hashcode = hashcode xor if(point.type == 1) blackZobrist[x][y] else whiteZobrist[x][y]
        return this.hashcode
    }

    /**
     * 获取当前局势的棋型（按顺序匹配）
     *
     * 如当前局势棋型为："210111002"（同时包含活三和冲四的棋型）
     * 该方法会优先匹配到活三 "011100"，然后返回该棋型
     * 满足冲四 "10111" 棋型，但由于顺序问题，将不会返回
     *
     * @param situation 当前局势
     * @return
     */
    private fun getChessModel(situation:String):ChessModel? {
        ChessModel.values().forEach {
            it.values.forEach { v ->
                if (situation.contains(v)) {
                    return it
                }
            }
        }
        return null
    }

    /**
     * 检查当前局势是否处于某个局势
     *
     * @param situation  当前局势
     * @param chessModel 检查的局势
     * @return
     */
    private fun checkSituation(situation:String,chessModel:ChessModel):Boolean {
        chessModel.values.forEach {
            if (situation.contains(it)) {
                return true
            }
        }
        return false
    }

    /**
     * 获取棋位局势
     *
     * @param point     当前棋位
     * @param direction 大方向 1.横 2.纵 3.左斜 4.右斜
     * @return
     */
    private fun getSituation(point:Point, direction:Int):String {
        // 下面用到了relativePoint函数，根据传入的四个大方向做转换
        val directionValue = direction * 2 - 1
        // 以下是将各个方向的棋子拼接成字符串返回
        val sb = StringBuilder()
        appendChess(sb, point, directionValue, 4)
        appendChess(sb, point, directionValue, 3)
        appendChess(sb, point, directionValue, 2)
        appendChess(sb, point, directionValue, 1)
        sb.append(Chess.BLACK_CHESS) // 当前棋子统一标记为1(黑)
        appendChess(sb, point, directionValue + 1, 1)
        appendChess(sb, point, directionValue + 1, 2)
        appendChess(sb, point, directionValue + 1, 3)
        appendChess(sb, point, directionValue + 1, 4)
        return sb.toString()
    }

    /**
     * 拼接各个方向的棋子
     *
     * 由于现有评估模型是对黑棋进行评估
     * 所以，为了方便对局势进行评估，如果当前是白棋方，需要将扫描到的白棋转换为黑棋，黑棋转换为白棋
     * 如：point(x=0,y=0,type=2) 即当前为白棋方
     * 扫描到的某个方向局势为：20212 -> 转换后 -> 10121
     *
     * @param sb        字符串容器
     * @param point     当前棋子
     * @param direction 方向 1.左横 2.右横 3.上纵 4.下纵  5.左斜上 6.左斜下 7.右斜上 8.右斜下
     * @param offset    偏移量
     */
    private fun appendChess(sb:StringBuilder, point:Point, direction:Int, offset:Int) {
        var chess:Int = relativePoint(point, direction, offset)
        if (chess > -1) {
            if (point.type == 2) {
                // 对白棋进行转换
                if (chess > 0) {
                    // 对棋子颜色进行转换，2->1，1->2
                    chess = 3 - chess
                }
            }
            sb.append(chess)
        }
    }

    /**
     * 获取相对点位棋子
     *
     * @param point     当前棋位
     * @param direction 方向 1.左横 2.右横 3.上纵 4.下纵  5.左斜上 6.左斜下 7.右斜上 8.右斜下
     * @param offset    偏移量
     * @return -1:越界 0:空位 1:黑棋 2:白棋
     */
    private fun relativePoint(point:Point, direction:Int, offset:Int):Int {
        var x = point.x
        var y = point.y
        when(direction) {
            1 -> x -= offset
            2 -> x += offset
            3 -> y -= offset
            4 -> y += offset
            5 -> {
                x += offset
                y -= offset
            }
            6 -> {
                x -= offset
                y += offset
            }
            7 -> {
                x -= offset
                y -= offset
            }
            8 -> {
                x += offset
                y += offset
            }
        }
        // 越界返回-1，否则返回该位置的棋子
        return if (x < 0 || y < 0 || x >= boardSize || y >= boardSize) -1
        else this.chessData[x][y]
    }

    private fun Point.list() = ArrayList<Point>().apply { add(this@list) }

}