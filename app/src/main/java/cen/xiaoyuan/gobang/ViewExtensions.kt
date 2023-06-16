package cen.xiaoyuan.gobang

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.databinding.BindingAdapter
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun View.fadeToVisibilityUnsafe(visible: Boolean, force: Boolean = false, gone: Boolean = false) {
    GlobalScope.launch(Dispatchers.Main.immediate) { fadeToVisibility(visible, force, gone) }
}
suspend fun View.fadeToVisibility(visible: Boolean, force: Boolean = false, gone: Boolean = false) {
    if (visible) {
        fadeIn(force)
    } else {
        fadeOut(force, gone)
    }
}
suspend fun View.fadeIn(force: Boolean = false) {
    if (!isVisible) {
        alpha = 0f
        isVisible = true
    }
    animate().run {
        alpha(1f)
        if (!(isLaidOut || force) || (isVisible && alpha == 1f)) {
            duration = 0
        } else {
            duration = context.shortAnimTime.toLong()
            interpolator = context.getInterpolator(android.R.interpolator.fast_out_slow_in)
        }
        start()
        awaitEnd()
    }
}
suspend fun View.fadeOut(force: Boolean = false, gone: Boolean = false) {
    animate().run {
        alpha(0f)
        if (!(isLaidOut || force) || (!isVisible || alpha == 0f)) {
            duration = 0
        } else {
            duration = context.shortAnimTime.toLong()
            interpolator = context.getInterpolator(android.R.interpolator.fast_out_linear_in)
        }
        start()
        awaitEnd()
    }
    if (gone) {
        isGone = true
    } else {
        isInvisible = true
    }
}

fun <T:View> T.treeObserver(block:((T)->Unit)) {
    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            block.invoke(this@treeObserver)
            this@treeObserver.viewTreeObserver.removeOnPreDrawListener(this)
            return true
        }
    })
}


fun View.doOnApplyWindowInsets(
    block: (v: View, insets: WindowInsetsCompat, padding: Insets, margins: Insets) -> WindowInsetsCompat
) {
    val padding = recordPadding()
    val margins = recordMargins()
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        block(view, insets, padding, margins)
    }
    requestApplyInsetsWhenAttached()
}

fun View.recordPadding() = Insets.of(paddingLeft, paddingTop, paddingRight, paddingBottom)

fun View.recordMargins(): Insets {
    val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return Insets.NONE
    return Insets.of(lp.leftMargin, lp.topMargin, lp.rightMargin, lp.bottomMargin)
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

inline val View.isRtl: Boolean get() = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL


@BindingAdapter(
    "paddingLeftSystemBars",
    "paddingTopSystemBars",
    "paddingRightSystemBars",
    "paddingBottomSystemBars",
    "marginLeftSystemBars",
    "marginTopSystemBars",
    "marginRightSystemBars",
    "marginBottomSystemBars",
    "marginBottomHarfSystemBars",
    requireAll = false
)
fun applySystemBars(
    view: View,
    padLeft: Boolean,
    padTop: Boolean,
    padRight: Boolean,
    padBottom: Boolean,
    marginLeft: Boolean,
    marginTop: Boolean,
    marginRight: Boolean,
    marginBottom: Boolean,
    marginBottomHarf: Boolean
) {
    val adjustPadding = padLeft || padTop || padRight || padBottom
    val adjustMargins = marginLeft || marginTop || marginRight || marginBottom||marginBottomHarf
    if (!(adjustPadding || adjustMargins)) {
        return
    }

    view.doOnApplyWindowInsets { v, insets, padding, margins ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        if (adjustPadding) {
            val systemLeft = if (padLeft) systemBars.left else 0
            val systemTop = if (padTop) systemBars.top else 0
            val systemRight = if (padRight) systemBars.right else 0
            val systemBottom = if (padBottom) systemBars.bottom else 0
            v.updatePadding(
                left = padding.left + systemLeft,
                top = padding.top + systemTop,
                right = padding.right + systemRight,
                bottom = padding.bottom + systemBottom,
            )
        }
        if (adjustMargins) {
            val systemLeft = if (marginLeft) systemBars.left else 0
            val systemTop = if (marginTop) systemBars.top else 0
            val systemRight = if (marginRight) systemBars.right else 0
            val systemBottom = if (marginBottom || marginBottomHarf) systemBars.bottom/(if(marginBottomHarf) 2 else 1) else 0
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = margins.left + systemLeft
                topMargin = margins.top + systemTop
                rightMargin = margins.right + systemRight
                bottomMargin = margins.bottom + systemBottom
            }

        }
        insets // 总是返回insets，以便children可以使用
    }
}