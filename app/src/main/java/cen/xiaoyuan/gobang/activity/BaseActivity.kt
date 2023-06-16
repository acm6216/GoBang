package cen.xiaoyuan.gobang.activity

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class BaseActivity<T:ViewBinding>: AppCompatActivity(),CoroutineScope {

    override val coroutineContext: CoroutineContext get() = Dispatchers.IO

    protected lateinit var sharedPreferences: SharedPreferences

    abstract fun setLayout():T
    protected lateinit var binding:T
    abstract fun created(savedInstanceState:Bundle?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayout().also { binding = it }.root)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        created(savedInstanceState)
    }

    protected inline fun AppCompatActivity.repeatWithViewLifecycle(
        minState: Lifecycle.State = Lifecycle.State.STARTED,
        crossinline block: suspend CoroutineScope.() -> Unit
    ) {
        if (minState == Lifecycle.State.INITIALIZED || minState == Lifecycle.State.DESTROYED) {
            throw IllegalArgumentException("minState must be between INITIALIZED and DESTROYED")
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(minState) {
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