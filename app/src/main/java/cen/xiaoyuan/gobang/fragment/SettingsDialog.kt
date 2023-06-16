package cen.xiaoyuan.gobang.fragment

import android.view.View
import cen.xiaoyuan.gobang.R
import cen.xiaoyuan.gobang.databinding.DialogSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsDialog:BaseDialog<DialogSettingsBinding>() {

    override fun setLayout(): DialogSettingsBinding = DialogSettingsBinding.inflate(layoutInflater,null,false)

    override val isCreateView: Boolean get() = false

    override fun MaterialAlertDialogBuilder.init(): MaterialAlertDialogBuilder{
        setTitle(R.string.action_settings)
        setPositiveButton(R.string.close,null)
        return this
    }

    override fun scrollView(): View = binding.scrollView

}