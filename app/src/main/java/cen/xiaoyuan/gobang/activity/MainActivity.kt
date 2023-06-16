package cen.xiaoyuan.gobang.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.graphics.Insets
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import cen.xiaoyuan.gobang.*
import cen.xiaoyuan.gobang.databinding.ActivityMainBinding
import cen.xiaoyuan.gobang.fragment.SaveDialog
import cen.xiaoyuan.gobang.fragment.SettingsDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    override fun setLayout(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
    private val main:MainViewModel by viewModels()

    override fun created(savedInstanceState: Bundle?) {
        binding.activityRoot.doOnApplyWindowInsets { v, insets, _, _ ->
            val isRtl = v.isRtl
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = if (isRtl) 0 else systemBars.left,
                right = if (isRtl) systemBars.right else 0,
                top = systemBars.top,
                bottom = systemBars.bottom
            )
            WindowInsetsCompat.Builder(insets).setInsets(
                WindowInsetsCompat.Type.systemBars(),
                Insets.of(
                    if (isRtl) systemBars.left else 0,
                    systemBars.top,
                    if (isRtl) 0 else systemBars.right,
                    systemBars.bottom
                )
            ).build()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding.rail.listener = {
            when (it) {
                R.id.action_undo -> main.undo()
                R.id.action_save -> main.save()
                R.id.action_settings -> SettingsDialog().show(supportFragmentManager,javaClass.simpleName)
                R.id.action_save_load -> SaveDialog().show(supportFragmentManager,javaClass.simpleName)
                R.id.action_new -> main.new()
                else -> about()
            }
        }

        repeatWithViewLifecycle {
            launch {
                main.toast.collect{ it.toast() }
            }
        }
    }

    private fun Int.toast(){
        Snackbar.make(binding.activityRoot,this,Snackbar.LENGTH_SHORT).show()
    }

    private fun about(){
        MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.about_title)
            setMessage(readText("about.txt"))
            setPositiveButton(R.string.about_ok,null)
            show()
        }
    }

}