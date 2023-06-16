package cen.xiaoyuan.gobang

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App: Application(){

    override fun onCreate() {
        super.onCreate()
        SpManager.init(this)
        setupDarkModePreference()
    }

    private fun setupDarkModePreference() {
        SpManager.listen(getString(R.string.set_key_dark_mode),javaClass.simpleName,SpManager.SpBoolListener{
            if (it) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        })
    }

}