package cen.xiaoyuan.gobang.drawable

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Meteor(
    private val widthPixels: Int,
    private val heightPixels: Int
):Drawable(),Animatable {

    private val random = Random.Default

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = getRandomColor()
        style = Paint.Style.FILL
        shader = LinearGradient(
            0f, 0f, 0f, 1000f,
            intArrayOf(Color.TRANSPARENT, Color.RED),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    private val paintRHead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val paintR = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(170, 255, 255, 255)
    }
    private val paintRLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(100, 255, 255, 255)
    }
    private val paintRLight2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(30, 255, 255, 255)
    }
    private val uiHandler = Handler(Looper.getMainLooper())
    private val sweep = 340.0
    private val meteorCount = 3
    private val starCount = 100
    private val stars = HashSet<Star>()
    private var timer:Timer? = null
    private var isRunning = false

    init {
        val color = getRandomColor()
        for (i in 0 until meteorCount) {
            val star = randomStar()
            star.color = color
            stars.add(star)
        }

        for (i in 0 until starCount) {
            val star = randomStar(Math.random().toFloat() * 1.5f + 0.1f)
            star.isPoint = true
            star.color = Color.WHITE
            stars.add(star)
        }
    }

    override fun start() {
        isRunning = true
        if(timer==null) timer = Timer()
        timer?.schedule(object :TimerTask(){
            override fun run() {
                uiHandler.post{
                    update()
                }
            }
        },16,16)
    }

    private fun update(){
        stars.forEach {
            it.x = it.x+it.vX
            it.y = it.y+it.vY
            if(it.isOutSite()){
                it.setXY()
            }
        }
        invalidateSelf()
    }

    private fun Star.setXY():Star{
        if(Math.random()>=0.5){
            x = 0f
            y = Math.random().toFloat()*heightPixels
        }else{
            x = Math.random().toFloat()*widthPixels
            y = 0f
        }
        if(!isPoint) color = getRandomColor()
        r = Math.random().toFloat()*3+0.3f
        return this
    }

    private fun Star.isOutSite():Boolean{
        return if(isPoint) x<0 || x>(widthPixels+ sin(sweep)) || y<0 || y>(heightPixels+ cos(sweep))
        else x<0 || x>(widthPixels+ sin(sweep)*l) || y<0 || y>(heightPixels+cos(sweep)*l)
    }

    override fun stop() {
        if(timer!=null){
            timer?.cancel()
            timer = null
        }
        isRunning = false
    }

    override fun isRunning(): Boolean = isRunning

    private fun randomStar(r:Float = Math.random().toFloat()*1.8f+0.3f)
        = Star(Math.random().toFloat()*widthPixels,Math.random().toFloat()*heightPixels,r)

    override fun draw(canvas: Canvas) {
        stars.forEach {
            if(it.isPoint){
                canvas.drawCircle(it.x,it.y,it.r,paintRHead.apply { color = it.color })
            }else{
                val path = it.getPath()
                canvas.drawPath(path,paint)
                canvas.drawCircle(it.x,it.y,it.headRLight2,paintRLight2)
                canvas.drawCircle(it.x,it.y,it.headRLight,paintRLight)
                canvas.drawCircle(it.x,it.y,it.headR,paintR)
            }
        }
    }

    private fun Star.getPath():Path{
        val startX = (x+r* cos(sweep)).toFloat()
        val startY = (y-r* sin(sweep)).toFloat()
        val twoX = (x-r* cos(sweep)).toFloat()
        val twoY = (y+r* sin(sweep)).toFloat()
        val threeX = (x - l* sin(sweep)).toFloat()
        val threeY = (y - l* cos(sweep)).toFloat()
        paintR.color = color.trans(170)
        paintRLight.color = color.trans(100)
        paintRLight2.color = color.trans(30)
        return Path().apply {
            moveTo(startX,startY)
            lineTo(twoX,twoY)
            lineTo(threeX,threeY)
            moveTo(startX,startY)
            close()
            paint.shader = LinearGradient(
                threeX, threeY, x, y,
                intArrayOf(Color.TRANSPARENT, color),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    private fun Int.trans(alpha:Int):Int = Color.argb(alpha,red,green,blue)

    override fun setAlpha(p0: Int) {}

    override fun setColorFilter(p0: ColorFilter?) {}

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun getRandomColor(): Int = Color.argb(255,random.nextInt(256),random.nextInt(256),random.nextInt(256))

    inner class Star(var x:Float,var y:Float,var r:Float){
        var color:Int = getRandomColor()
        val vX get() = r*1.5f
        val vY get() = r*2f
        val l get() = r*10 + x*0.3f
        val headR get() = r+r*0.3f
        val headRLight get() = r+r*2f
        val headRLight2 get() = headRLight+headRLight*0.6f
        var isPoint = false
    }
}