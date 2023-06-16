package cen.xiaoyuan.gobang.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import cen.xiaoyuan.gobang.R
import cen.xiaoyuan.gobang.data.Chess
import cen.xiaoyuan.gobang.data.GoBangGame
import com.google.gson.Gson

class GoBangMiniView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var borderSize = 2f
    private val borderPadding = 0.dp
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colorControlNormal()
        strokeWidth = borderSize
    }
    private val chessPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
    }

    var boardSize = 0
    var singleGridSize = 0f

    private val allChess = ArrayList<Chess>()

    fun setData(game:GoBangGame,gson: Gson){
        boardSize = game.boardSize
        singleGridSize = (measuredHeight-borderPadding*2) / (boardSize-1)
        allChess.clear()
        allChess.addAll(gson.fromJson(game.content,Array<Chess>::class.java).toMutableList())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        repeat(boardSize) { row -> canvas.lineRow(row) }
        repeat(boardSize) { col -> canvas.lineCol(col) }
        canvas.drawAllChessFill()
        canvas.drawAllChessStroke()
    }

    private fun Paint.whiteChessFill(): Paint {
        color = 0xffffffff.toInt()
        style = Paint.Style.FILL
        return this
    }

    private fun Paint.blankChessFill(): Paint {
        color = 0xff000000.toInt()
        style = Paint.Style.FILL
        return this
    }

    private fun Canvas.drawAllChessStroke() {
        val radius = singleGridSize / 12 * 5
        allChess.forEach {
            drawCircle(
                it.y * singleGridSize+borderPadding, it.x * singleGridSize+borderPadding, radius,
                if (it.isWhite) chessPaint.whiteChessStroke() else chessPaint.blankChessStroke()
            )
        }
    }

    private fun Paint.whiteChessStroke(): Paint {
        color = 0xff000000.toInt()
        style = Paint.Style.STROKE
        return this
    }

    private fun Paint.blankChessStroke(): Paint {
        color = 0xffffffff.toInt()
        style = Paint.Style.STROKE
        return this
    }

    private fun Canvas.drawAllChessFill(){
        val radius = singleGridSize / 12 * 5
        allChess.forEach {
            drawCircle(
                it.y * singleGridSize+borderPadding, it.x * singleGridSize+borderPadding, radius,
                if (it.isWhite) chessPaint.whiteChessFill() else chessPaint.blankChessFill()
            )
        }
    }

    private fun Canvas.lineCol(col: Int): Canvas {
        drawLine(
            singleGridSize * col +borderPadding, borderPadding,
            singleGridSize * col +borderPadding, measuredHeight - borderPadding,
            paint
        )
        return this
    }

    private fun Canvas.lineRow(row: Int): Canvas {
        drawLine(
            borderSize / 2f+borderPadding, singleGridSize * row +borderPadding,
            measuredWidth + borderSize / 2f-borderPadding, singleGridSize * row +borderPadding,
            paint
        )
        return this
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val value = measureSize(widthMeasureSpec)
        setMeasuredDimension(value, value)
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