package cen.xiaoyuan.gobang.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Magnifier
import cen.xiaoyuan.gobang.R
import cen.xiaoyuan.gobang.data.Chess
import cen.xiaoyuan.gobang.data.ChessBoard
import cen.xiaoyuan.gobang.data.GameOver
import kotlin.math.sqrt

class GoBangView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isHorizontal = false

    init {
        val typedArray = context.obtainStyledAttributes(attrs,R.styleable.GoBangView)
        if(typedArray.hasValue(R.styleable.GoBangView_is_horizontal)) {
            isHorizontal = typedArray.getBoolean(R.styleable.GoBangView_is_horizontal, false)
        }
        typedArray.recycle()
    }

    var gameOver: GameOver? = null

    private var magnifier:Magnifier?=null

    private var isTouching = false
    private var isEnableTouch = true

    //落子监听
    private var listener: ((Chess) -> Unit)? = null
    fun listener(block: ((Chess) -> Unit)) {
        listener = block
    }

    private var borderSize = 4
    private val borderPadding = 8.dp

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = borderSize.toFloat()
        style = Paint.Style.STROKE
        color = colorControlNormal()
    }

    private val winnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = borderSize.toFloat() * 3
        style = Paint.Style.STROKE
        color = 0xff00ff00.toInt()
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorControlNormal()
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x00ffffff
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorControlNormal()
    }
    private val seqTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xffff0000.toInt()
    }
    private val chessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 3f
    }
    private var isWhiteChess = false

    private var singleGridSize = 0f

    var showMagnifier = true

    var chessLastSequence = false
        set(value) {
            field = value
            invalidate()
        }
    var chessSequence = true
        set(value) {
            field = value
            invalidate()
        }
    var chessBoard: ChessBoard = ChessBoard.ChessBoard15()
        set(value) {
            motionEvent = null
            gameOver = null
            singleGridSize = (measuredHeight-borderPadding*2) / value.id
            textPaint.textSize = singleGridSize / 10f * 4
            seqTextPaint.textSize = singleGridSize / 10f * 4
            field = value
            invalidate()
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            magnifier = Magnifier(this)
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            magnifier = Magnifier.Builder(this)
                .setClippingEnabled(false)
                .setCornerRadius(20f)
                .build()
        }
    }

    private val allChess get() = chessBoard.allChess
    private val chessStatus get() = chessBoard.chessStatus

    private var motionEvent: MotionEvent? = null

    fun setChessBoardByValue(size:Int){
        chessBoard = when(size){
            15 -> ChessBoard.ChessBoard15()
            17 -> ChessBoard.ChessBoard17()
            else -> ChessBoard.ChessBoard19()
        }
    }

    fun isEnable(enable: Boolean) {
        isEnableTouch = enable
        isEnabled = enable
    }

    fun load(){
        gameOver = null
        motionEvent = null
        isEnableTouch = true
        invalidate()
    }

    fun clearStatus(){
        chessBoard.clearStatus()
        load()
    }
    fun gameOver(game: GameOver) {
        gameOver = game
        invalidate()
    }

    //悔棋
    fun undo(){
        motionEvent = null
        gameOver = null
        if(allChess.isNotEmpty()){
            allChess.removeLast().undo()
            if(allChess.size%2==1 && allChess.isNotEmpty())
                allChess.removeLast().undo()
        }
        isEnableTouch = true
        invalidate()
    }

    private fun Chess.undo(){
        chessStatus[x*chessBoard.size+y] = Chess.EMPTY_CHESS
    }

    fun play(chess: Chess) {
        if (chessStatus[chess.x * chessBoard.size + chess.y] == Chess.EMPTY_CHESS) {
            chessStatus[chess.x * chessBoard.size + chess.y] = chess.value
            allChess.add(chess)
            isEnableTouch = true
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        isTouching = event.action != MotionEvent.ACTION_UP
        motionEvent = if(isEnableTouch) event else null
        when(event.action){
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> event.showMagnifier()
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> dismissMagnifier()
        }
        invalidate()
        return true
    }

    private fun MotionEvent.showMagnifier(){
        if(!showMagnifier) return
        val viewPosition = IntArray(2)
        getLocationOnScreen(viewPosition)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            magnifier?.show(rawX - viewPosition[0] - 20, rawY - viewPosition[1] - 20)
        }
    }

    private fun dismissMagnifier(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            magnifier?.dismiss()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawGoBangBackground()
        if(allChess.isNotEmpty() && gameOver==null){
            canvas.drawLastChess(allChess.last())
        }
        if(isEnableTouch) motionEvent?.also { canvas.drawChess(it) }
        canvas.drawAllChessStroke()
        gameOver?.also { canvas.chessLine(it) }
        canvas.drawAllChessFill()
    }

    private fun Float.xRange(): Float = when {
        this < singleGridSize -> singleGridSize
        this > measuredWidth - singleGridSize -> measuredWidth.toFloat() - singleGridSize
        else -> this
    }

    private fun Float.yRange(): Float = when {
        this < singleGridSize -> singleGridSize
        this > measuredHeight - singleGridSize -> measuredHeight.toFloat() - singleGridSize
        else -> this
    }

    private fun Float.xRow(): Int = (this / singleGridSize).toInt()
    private fun Float.yCol(): Int = (this / singleGridSize).toInt()
    private fun Float.xTrans(): Float = xRow() * singleGridSize
    private fun Float.yTrans(): Float = yCol() * singleGridSize

    private fun Canvas.chessLine(game: GameOver) {
        drawLine(
            game.start.row * singleGridSize+borderPadding, game.start.col * singleGridSize+borderPadding,
            game.end.row * singleGridSize+borderPadding, game.end.col * singleGridSize+borderPadding,
            winnerPaint
        )
    }

    private fun Canvas.drawLastChess(chess: Chess):Canvas{
        val radius = singleGridSize / 12 * 5
        val d = sqrt(radius*radius*2)/10*7
        drawLine(
            chess.row*singleGridSize-d+borderPadding,chess.col*singleGridSize-d+borderPadding,
            chess.row*singleGridSize+d+borderPadding,chess.col*singleGridSize+d+borderPadding,
            winnerPaint
        )
        drawLine(
            chess.row*singleGridSize+d+borderPadding,chess.col*singleGridSize-d+borderPadding,
            chess.row*singleGridSize-d+borderPadding,chess.col*singleGridSize+d+borderPadding,
            winnerPaint
        )
        return this
    }

    private fun Canvas.drawAllChessStroke() {
        val radius = singleGridSize / 12 * 5
        allChess.forEach {
            drawCircle(
                it.row * singleGridSize+borderPadding, it.col * singleGridSize+borderPadding, radius,
                if (it.isWhite) chessPaint.whiteChessStroke() else chessPaint.blankChessStroke()
            )
        }
    }

    private fun Canvas.drawAllChessFill() {
        val radius = singleGridSize / 12 * 5
        allChess.forEachIndexed { index, it ->
            drawCircle(
                it.row * singleGridSize+borderPadding, it.col * singleGridSize+borderPadding, radius,
                if (it.isWhite) chessPaint.whiteChessFill() else chessPaint.blankChessFill()
            )
            if (chessSequence) drawChessSequence(index + 1, it)

        }
        if (!chessSequence && chessLastSequence && allChess.isNotEmpty())
            drawChessSequence(allChess.size,allChess.last())
    }

    private fun Canvas.drawChessSequence(seq: Int, chess: Chess): Canvas {
        val text = seq.toString()
        val textH = seqTextPaint.textHeight(text)
        val textW = seqTextPaint.textWidth(text)
        drawText(
            seq.toString(),
            chess.row * singleGridSize - textW / 2f+borderPadding,
            chess.col * singleGridSize - textH / 2f+borderPadding,
            seqTextPaint
        )
        return this
    }

    private fun Canvas.drawChess(event: MotionEvent) {
        val viewPosition = IntArray(2)
        getLocationOnScreen(viewPosition)
        val cx = (event.rawX - viewPosition[0]).xRange().xTrans()
        val cy = (event.rawY - viewPosition[1]).yRange().yTrans()
        val radius = singleGridSize / 12 * 5

        drawCircle(
            cx+borderPadding, cy+borderPadding, radius,
            if (isWhiteChess) chessPaint.whiteChessFill() else chessPaint.blankChessFill()
        )
        drawCircle(
            cx+borderPadding, cy+borderPadding, radius,
            if (isWhiteChess) chessPaint.whiteChessStroke() else chessPaint.blankChessStroke()
        )

        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            val row = (event.rawX - viewPosition[0]).xRange().xRow() - 1
            val col = (event.y - viewPosition[1]).yRange().yCol() - 1
            if (chessStatus[col * chessBoard.size + row] == Chess.EMPTY_CHESS
                && (event.rawX.toInt()-viewPosition[0] in borderPadding.toInt() until measuredWidth-borderPadding.toInt())
                && (event.rawY.toInt()-viewPosition[1] in borderPadding.toInt() until measuredHeight-borderPadding.toInt())) {
                Chess(isWhiteChess, col + 1, row + 1).apply {
                    chessStatus[col * chessBoard.size + row] = this.value
                    allChess.add(this)
                    listener?.invoke(this)
                }
                isEnableTouch = false
            }else {
                motionEvent = null
                invalidate()
            }
        }
    }

    private fun Paint.whiteChessFill(): Paint {
        color = 0xffffffff.toInt()
        style = Paint.Style.FILL
        return this
    }

    private fun Paint.whiteChessStroke(): Paint {
        color = 0xff000000.toInt()
        style = Paint.Style.STROKE
        return this
    }

    private fun Paint.blankChessFill(): Paint {
        color = 0xff3e3e3e.toInt()
        style = Paint.Style.FILL
        return this
    }

    private fun Paint.blankChessStroke(): Paint {
        color = 0xffffffff.toInt()
        style = Paint.Style.STROKE
        return this
    }

    private fun Canvas.textRow(text: String, row: Int): Canvas {
        drawText(
            text,
            measuredWidth - textPaint.textWidth(text) - textPaint.textSize / 2-borderPadding,
            singleGridSize * (row + 1) - textPaint.textHeight(text)/2 + borderPadding,
            textPaint
        )
        return this
    }

    private fun Canvas.lineRow(row: Int): Canvas {
        drawLine(
            singleGridSize - borderSize / 2f+borderPadding, singleGridSize * (row + 1)+borderPadding,
            measuredWidth - singleGridSize + borderSize / 2f-borderPadding, singleGridSize * (row + 1)+borderPadding,
            linePaint
        )
        return this
    }

    private fun Canvas.textCol(text: String, col: Int): Canvas {
        drawText(
            text,
            singleGridSize * (col + 1) - textPaint.textSize / 2 + borderSize+borderPadding,
            measuredHeight - textPaint.textHeight(text) - textPaint.textSize * 1.3f-borderPadding,
            textPaint
        )
        return this
    }

    private fun Canvas.lineCol(col: Int): Canvas {
        drawLine(
            singleGridSize * (col + 1)+borderPadding, singleGridSize+borderPadding,
            singleGridSize * (col + 1)+borderPadding, measuredHeight - singleGridSize-borderPadding,
            linePaint
        )
        return this
    }

    private fun Canvas.rect(paint: Paint): Canvas {
        drawRect(
            borderPadding+borderSize / 2f, borderPadding+borderSize / 2f, measuredWidth - borderSize / 2f-borderPadding,
            measuredHeight - borderSize / 2f-borderPadding, paint
        )
        return this
    }

    private fun Canvas.drawGoBangBackground() {
        rect(bgPaint)
        rect(linePaint)
        repeat(chessBoard.size) { row -> lineRow(row).textRow((row + 1).toString(), row) }
        repeat(chessBoard.size) { col -> lineCol(col).textCol(('A' + col).toString(), col) }
        drawPoint()
    }

    private fun Canvas.drawPoint(x:Float,y:Float,dx:Int,dy:Int,offset:Int,radius:Float){
        drawCircle(
            x + dx*offset * singleGridSize+dx*borderPadding,
            y + dy*offset * singleGridSize+dy*borderPadding,
            radius, pointPaint
        )
    }
    private fun Canvas.drawPoint(): Canvas {
        val offset = chessBoard.size / 4
        val radius = borderSize * 3f
        drawPoint(singleGridSize,singleGridSize,1,1,offset,radius)
        drawPoint(singleGridSize,measuredHeight - singleGridSize,1,-1,offset,radius)
        drawPoint(measuredWidth - singleGridSize,singleGridSize,-1,1,offset,radius)
        drawPoint(measuredWidth - singleGridSize,measuredHeight - singleGridSize,-1,-1,offset,radius)
        drawPoint(measuredWidth/2f,measuredHeight/2f,0,0,offset,radius)
        return this
    }

    private fun Paint.textHeight(text: String): Int {
        val result = Rect()
        getTextBounds(text, 0, text.length, result)
        return result.top - result.bottom
    }

    private fun Paint.textWidth(text: String): Int {
        val result = Rect()
        getTextBounds(text, 0, text.length, result)
        return result.right - result.left
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val spec = if(isHorizontal) heightMeasureSpec else widthMeasureSpec
        super.onMeasure(spec, spec)
        val value = measureSize(spec)
        setMeasuredDimension(value, value)
        singleGridSize = (measuredHeight-borderPadding*2) / chessBoard.id
        textPaint.textSize = singleGridSize / 10f * 4
        seqTextPaint.textSize = singleGridSize / 10f * 4
    }

    private fun measureSize(measureSpec: Int): Int = MeasureSpec.getSize(measureSpec)

    private fun colorControlNormal(): Int {
        val typedValue = TypedValue()
        val a =
            context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorControlNormal))
        val color = a.getColor(0, Color.BLUE)
        a.recycle()
        return color
    }

    private inline val Int.dp: Float get() = run { toFloat().dp }
    private inline val Float.dp: Float
        get() = run {
            val scale: Float = context.resources.displayMetrics.density
            this * scale + 0.5f
        }

}