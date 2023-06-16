package cen.xiaoyuan.gobang.fragment

import android.content.res.Configuration
import android.os.Bundle
import androidx.preference.SwitchPreferenceCompat
import cen.xiaoyuan.gobang.R

class SettingsFragment: BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
        R.string.set_key_dark_mode.get<SwitchPreferenceCompat>()?.setIcon(if(isDarkMode()) R.drawable.twotone_dark_mode else R.drawable.twotone_light_mode)
    }

    private fun isDarkMode():Boolean = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

}