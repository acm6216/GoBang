package cen.xiaoyuan.gobang.drawable

import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.math.cos
import kotlin.math.sin

/**
 * https://github.com/auv1107/huaxia.widget
 */

class StarrySky(private val widthPixels: Int, private val heightPixels: Int) : Drawable(), Animatable {
    private val starPaint = Paint().apply {
        color = Color.parseColor("#ADFFC107")
    }
    private val stars = HashSet<Star>()
    var backgroundColor: Int = Color.TRANSPARENT

    private val LOCK = Any()
    fun addStar(star: Star) {
        synchronized(LOCK) {
            stars.add(star)
        }
    }
    fun removeStar(star: Star) {
        synchronized(LOCK) {
            stars.remove(star)
        }
    }
    fun copyStar(): HashSet<Star> {
        synchronized(LOCK) {
            val set = HashSet<Star>()
            set.addAll(stars)
            return set
        }
    }
    fun addRandomStar() {
        addStar(
            Star(
                random(0, widthPixels),
                random(0, heightPixels),
                random(0, MAX_SPEED).toInt(),
                random(0, 360).toInt()
            )
        )
    }
    private var onStarOutListener: ((star: Star) -> Unit)? = null
    fun setOnStarOutListener(listener: ((star: Star) -> Unit)) {
        onStarOutListener = listener
    }

    fun random(start: Float, end: Float): Float {
        return (Math.random() * (end - start) + start).toFloat()
    }
    private fun random(start: Int, end: Int): Float {
        return (Math.random() * (end - start) + start).toFloat()
    }

    companion object {
        const val MAX_SPEED = 50
        class Star(
                var x: Float,
                var y: Float,
                var speed: Int, // pixels per second
                var direction: Int  // degree (0-360)
        ) {
            private val id: Int = getStarId()
            companion object {
                private var starId: Int = 0
                fun getStarId(): Int {
                    return starId++
                }
            }
            /**
             * 移动自己
             * delta -  过去的时间
             */
            fun move(delta: Int) {
                x += speed * delta / 1000f * cos(direction.toFloat())
                y += speed * delta / 1000f * sin(direction.toFloat())
            }

            override fun equals(other: Any?): Boolean {
                return other is Star && other.id == id
            }

            override fun hashCode(): Int {
                return id
            }
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawColor(backgroundColor)
        val currentStars = copyStar()
        for (star in currentStars) {
            canvas.drawCircle(star.x, star.y, 2f, starPaint)
        }
    }

    override fun setAlpha(alpha: Int) {}

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {}
    override fun getIntrinsicWidth(): Int {
        return widthPixels
    }

    override fun getIntrinsicHeight(): Int {
        return heightPixels
    }

    override fun isRunning(): Boolean {
        return isRunning
    }

    fun update(delta: Int) {
        val outSiteList = ArrayList<Star>()
        val currentStars = copyStar()
        for (star in currentStars) {
            star.move(delta)
            if (isOutSite(star)) {
                outSiteList.add(star)
            }
        }
        for (star in outSiteList) {
            onStarOutListener?.invoke(star)
        }
        invalidateSelf()
    }
    private var timer = Timer()
    private var isRunning = false
    private fun isOutSite(star: Star): Boolean {
        return star.x < 0 || star.x > widthPixels || star.y < 0 || star.y > heightPixels
    }

    private var lastTime = 0L
    override fun start() {
        isRunning = true
        lastTime = System.currentTimeMillis()
        update(0)

        timer.cancel()
        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                uiHandler.post {
                    val currentTime = System.currentTimeMillis()
                    update((currentTime - lastTime).toInt())
                    lastTime = currentTime
                }
            }
        }, 16, 16)
    }

    private val uiHandler = Handler(Looper.getMainLooper())

    override fun stop() {
        timer.cancel()
        isRunning = false
    }
}