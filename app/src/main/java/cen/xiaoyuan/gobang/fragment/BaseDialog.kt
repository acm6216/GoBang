package cen.xiaoyuan.gobang.fragment

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseDialog<T:ViewBinding>: DialogFragment() {

    protected lateinit var binding:T
    abstract fun setLayout():T
    abstract val isCreateView:Boolean
    open fun dialogCreated(){}
    abstract fun scrollView():View?

    abstract fun MaterialAlertDialogBuilder.init():MaterialAlertDialogBuilder

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireContext(),theme)
            .setCancelable(false)
            .init()
            .create()
        return dialog.apply {
            setView(setLayout().also {
                binding = it
            }.root)
            WindowCompat.setDecorFitsSystemWindows(requireNotNull(window), false)
            dialogCreated()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                scrollView()?.scrollIndicators = View.SCROLL_INDICATOR_BOTTOM or View.SCROLL_INDICATOR_TOP
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = if(isCreateView) setLayout().root else null

    protected inline fun DialogFragment.repeatWithViewLifecycle(
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline block: suspend CoroutineScope.() -> Unit
    ) {
        if (minState == Lifecycle.State.INITIALIZED || minState == Lifecycle.State.DESTROYED) {
            throw IllegalArgumentException("minState must be between INITIALIZED and DESTROYED")
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(minState) {
                block()
            }
        }
    }

    protected inline val Int.dp: Float get() = run { toFloat().dp }
    protected inline val Float.dp: Float
        get() = run {
            val scale: Float = resources.displayMetrics.density
            this * scale + 0.5f
        }

}