package cen.xiaoyuan.gobang.drawable

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.animation.LinearInterpolator
import cen.xiaoyuan.gobang.R
import kotlin.math.cos
import kotlin.math.sin

class TimerDrawable(
    private val size:Int,
    private val context: Context
):Drawable(), Animatable {

    private val x0 = size/2f
    private val y0 = size/2f
    private val padding = 4.dp
    private val radius = size/2-padding
    private val stroke = 2.dp

    private var isRunning = false
    private var x = x0
    private var y = stroke+2f

    private val valueAnimator by lazy(LazyThreadSafetyMode.NONE) {
        ValueAnimator.ofFloat(0f, 2f).apply {
            duration = 15000
            setFloatValues(0f, 360f)
            interpolator = LinearInterpolator()
            addUpdateListener {
                if (this@TimerDrawable.isRunning) {
                    val angle = it.animatedValue as Float
                    x = angle.circleX(radius,1f)
                    y = angle.circleY(radius,1f)
                    invalidateSelf()
                }
            }
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = colorControlNormal()
    }

    private fun Float.circleX(r:Float = radius,base:Float = 2f):Float = (x0+(r-stroke*base)* cos(this*Math.PI/180f)).toFloat()
    private fun Float.circleY(r:Float = radius,base:Float = 2f):Float = (y0+(r-stroke*base)* sin(this*Math.PI/180f)).toFloat()

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(x0,y0,radius,paint)
        val angle1 = 20f
        val angle2 = 50f
        canvas.drawLine(
            (-90f-angle1).circleX(radius+stroke*4),
            (-90f-angle1).circleY(radius+stroke*4),
            (-90f-angle2).circleX(radius+stroke*4),
            (-90f-angle2).circleY(radius+stroke*4),
            paint
        )
        canvas.drawLine(
            (-90f+angle1).circleX(radius+stroke*4),
            (-90f+angle1).circleY(radius+stroke*4),
            (-90f+angle2).circleX(radius+stroke*4),
            (-90f+angle2).circleY(radius+stroke*4),
            paint
        )
        canvas.drawLine(x0,y0,size/2f,size/2f-padding-stroke/3f,paint)
        canvas.drawCircle(x0,y0,1f,paint)
        canvas.drawLine(x0,y0,x,y,paint)
    }

    override fun setAlpha(p0: Int) {}

    override fun setColorFilter(p0: ColorFilter?) {}

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun start() {
        isRunning = true
        if(valueAnimator.isPaused)
            valueAnimator.resume()
        else valueAnimator.start()
    }

    override fun stop() {
        if(valueAnimator.isRunning)
            valueAnimator.pause()
        else if(valueAnimator.isPaused) valueAnimator.resume()
        isRunning = false
    }

    override fun isRunning(): Boolean = isRunning

    override fun getIntrinsicWidth(): Int = size

    override fun getIntrinsicHeight(): Int = size

    private inline val Int.dp: Float get() = run { toFloat().dp }
    private inline val Float.dp: Float
        get() = run {
            val scale: Float = context.resources.displayMetrics.density
            this * scale + 0.5f
        }

    private fun colorControlNormal(): Int {
        val typedValue = TypedValue()
        val a =
            context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorControlNormal))
        val color = a.getColor(0, Color.BLUE)
        a.recycle()
        return color
    }
}