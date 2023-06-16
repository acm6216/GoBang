package cen.xiaoyuan.gobang

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import androidx.annotation.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.TintTypedArray
import androidx.core.content.res.use
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun Context.getInterpolator(@InterpolatorRes id: Int): Interpolator =
    AnimationUtils.loadInterpolator(this,id)

fun Context.getInteger(@IntegerRes id: Int) = resources.getInteger(id)

fun Context.readText(name:String) = assets.open(name).bufferedReader().use { it.readText() }

val Context.shortAnimTime: Int
    get() = getInteger(android.R.integer.config_shortAnimTime)

val Context.layoutInflater: LayoutInflater
    get() = LayoutInflater.from(this)

@SuppressLint("Recycle")
fun Context.themeInterpolator(@AttrRes attr: Int): Interpolator {
    return AnimationUtils.loadInterpolator(
        this,
        obtainStyledAttributes(intArrayOf(attr)).use {
            it.getResourceId(0, android.R.interpolator.fast_out_slow_in)
        }
    )
}

@ColorInt
fun Context.getColorByAttr(@AttrRes attr: Int): Int{
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}