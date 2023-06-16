package cen.xiaoyuan.gobang

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import cen.xiaoyuan.gobang.view.RailView
import com.google.android.material.snackbar.Snackbar

class MoveUpwardBehavior @JvmOverloads constructor(
    context: Context? = null,
    attrs: AttributeSet? = null
) : CoordinatorLayout.Behavior<View>(context, attrs) {

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        if(child is RailView) {
            if(!child.isHorizontal) return true
            val translationY = 0f.coerceAtMost(dependency.translationY - dependency.height)
            child.translationY = translationY
        }
        return true
    }

    override fun onDependentViewRemoved(parent: CoordinatorLayout, child: View, dependency: View) {
        ViewCompat.animate(child).translationY(0f).start()
    }

}