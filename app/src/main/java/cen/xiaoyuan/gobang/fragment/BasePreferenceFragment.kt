package cen.xiaoyuan.gobang.fragment

import android.os.Bundle
import android.view.View
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {

    private fun setAllPreferencesToAvoidHavingExtraSpace(preference: Preference) {
        preference.isIconSpaceReserved = true
        if (preference is PreferenceGroup)
            preference.forEach {
                setAllPreferencesToAvoidHavingExtraSpace(it)
            }
    }

    override fun setPreferenceScreen(preferenceScreen: PreferenceScreen) {
        setAllPreferencesToAvoidHavingExtraSpace(preferenceScreen)
        super.setPreferenceScreen(preferenceScreen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this::class.java.superclass.superclass.getDeclaredField("mList").run {
            isAccessible = true
            (get(this@BasePreferenceFragment) as RecyclerView).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
            }
        }
    }

    protected fun <T : Preference> Int.get(): T? = if(context!=null) findPreference(getString(this)) else null

}