package cen.xiaoyuan.gobang.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class BaseFragment<T:ViewBinding>:Fragment(),CoroutineScope{

    override val coroutineContext: CoroutineContext get() = Dispatchers.IO

    abstract fun setLayout():T
    protected lateinit var binding: T

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = setLayout().also {
        binding=it
    }.root

    fun navigate(id:Int) = findNavController().navigate(id)

    protected inline fun Fragment.repeatWithViewLifecycle(
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

    protected fun <T:Any> CoroutineScope.launchCollect(flow: Flow<T>, block:((T)->Unit)){
        launch { flow.collect{ block(it) } }
    }

    protected inline val Int.dp: Float get() = run { toFloat().dp }
    protected inline val Float.dp: Float
        get() = run {
            val scale: Float = resources.displayMetrics.density
            this * scale + 0.5f
        }

}